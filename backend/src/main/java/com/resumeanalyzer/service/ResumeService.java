package com.resumeanalyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.dto.AnalysisResponseDto;
import com.resumeanalyzer.entity.AnalysisResult;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.entity.User;
import com.resumeanalyzer.exception.BadRequestException;
import com.resumeanalyzer.repository.AnalysisResultRepository;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class ResumeService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private final ResumeRepository resumes; private final AnalysisResultRepository analyses; private final CurrentUser currentUser;
    private final ChatClient.Builder chatClientBuilder; private final ObjectMapper json;
    @Value("${app.storage.location}") private Path storageRoot;

    @Transactional
    public AnalysisResponseDto uploadAndAnalyze(MultipartFile file, String jobDescription) {
        validate(file); User user = currentUser.require(); String key = user.getId()+"/"+ UUID.randomUUID()+extension(file.getOriginalFilename());
        Path root = storageRoot.toAbsolutePath().normalize(), stored = root.resolve(key).normalize();
        if (!stored.startsWith(root)) throw new BadRequestException("Invalid storage path");
        try {
            Files.createDirectories(stored.getParent()); file.transferTo(stored);
            String rawText = new Tika().parseToString(stored); if (rawText.isBlank()) throw new BadRequestException("No readable text was found in the document");
            Resume resume = resumes.save(Resume.builder().user(user).fileName(safeName(file.getOriginalFilename())).storageKey(key).rawText(rawText).build());
            AiAnalysis ai;
            try { ai = analyze(rawText, jobDescription); }
            catch (RuntimeException exception) { log.warn("Groq analysis unavailable for resume {}: {}", resume.getId(), exception.getMessage()); ai = deterministicFallback(rawText); }
            AnalysisResult result = analyses.saveAndFlush(AnalysisResult.builder().resume(resume).overallScore(score(ai.overallScore())).atsMatchPercentage(score(ai.atsMatchScore())).strengthsJson(toJson(ai.strengths())).weaknessesJson(toJson(ai.weaknesses())).missingKeywordsJson(toJson(ai.missingKeywords())).build());
            return new AnalysisResponseDto(resume.getId(), resume.getFileName(), result.getOverallScore(), result.getAtsMatchPercentage(), ai.strengths(), ai.weaknesses(), ai.missingKeywords(), result.getAnalyzedAt());
        } catch (IOException | TikaException ex) { throw new BadRequestException("The resume could not be parsed or stored", ex); }
    }
    private AiAnalysis analyze(String resume, String job) {
        String target = job == null || job.isBlank() ? "No target job description was supplied." : job;
        AiAnalysis answer = chatClientBuilder.build().prompt().system("You are an expert ATS resume analyst. Return only the requested structured result. Scores must be integers from 0 to 100. Do not invent experience.").user(u -> u.text("Analyze this resume against the job description. Return overallScore, atsMatchScore, strengths, weaknesses, and missingKeywords.\n\nRESUME:\n{resume}\n\nJOB DESCRIPTION:\n{job}").param("resume", resume).param("job", target)).call().entity(AiAnalysis.class);
        if (answer == null) throw new IllegalStateException("AI analysis returned no result"); return answer.normalized();
    }
    private AiAnalysis deterministicFallback(String text) {
        List<String> skills = List.of("communication", "problem solving", "teamwork").stream().filter(skill -> text.toLowerCase().contains(skill)).toList();
        return new AiAnalysis(50, 50, skills.isEmpty() ? List.of("Resume text was extracted successfully") : skills, List.of("AI analysis is temporarily unavailable; retry later for tailored feedback"), List.of());
    }
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("A non-empty resume file is required");
        if (file.getSize() > MAX_FILE_SIZE) throw new BadRequestException("Resume files must be 10 MB or smaller");
        String name = Objects.requireNonNullElse(file.getOriginalFilename(), "").toLowerCase();
        String contentType = Objects.requireNonNullElse(file.getContentType(), "");
        if (!(name.endsWith(".pdf") || name.endsWith(".docx")) || (!contentType.isBlank() && !"application/octet-stream".equals(contentType) && !TYPES.contains(contentType))) throw new BadRequestException("Only PDF and DOCX resumes are supported");
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(4); boolean pdf = header.length == 4 && header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
            boolean zip = header.length >= 2 && header[0] == 'P' && header[1] == 'K';
            if ((name.endsWith(".pdf") && !pdf) || (name.endsWith(".docx") && !zip)) throw new BadRequestException("The uploaded file content does not match its extension");
        } catch (IOException exception) { throw new BadRequestException("Could not inspect the uploaded file", exception); }
    }
    private String safeName(String name) { return Paths.get(Objects.requireNonNullElse(name,"resume")).getFileName().toString(); }
    private String extension(String name) { return Objects.requireNonNullElse(name,"").toLowerCase().endsWith(".docx") ? ".docx" : ".pdf"; }
    private int score(Integer value) { return Math.max(0,Math.min(100,Objects.requireNonNullElse(value,0))); }
    private String toJson(List<String> values) { try { return json.writeValueAsString(values); } catch (JsonProcessingException e) { throw new BadRequestException("Analysis result serialization failed",e); } }
    public record AiAnalysis(Integer overallScore,Integer atsMatchScore,List<String> strengths,List<String> weaknesses,List<String> missingKeywords) { AiAnalysis normalized() { return new AiAnalysis(overallScore,atsMatchScore,List.copyOf(Objects.requireNonNullElse(strengths,List.of())),List.copyOf(Objects.requireNonNullElse(weaknesses,List.of())),List.copyOf(Objects.requireNonNullElse(missingKeywords,List.of()))); } }
}

package com.resumeanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.dto.JobMatchDto;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class JobMatchService {

    private static final Pattern WORDS = Pattern.compile("[a-zA-Z][a-zA-Z+#.-]{3,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "with", "this", "that", "from", "your", "have", "will", "into", "using",
            "work", "years", "team", "role", "about", "more", "than", "they", "their",
            "and", "the", "for", "you", "are", "was", "were", "not", "our", "all"
    );

    private final ResumeRepository resumes;
    private final CurrentUser currentUser;
    private final RestClient jobsClient = RestClient.builder()
            .baseUrl("https://www.arbeitnow.com/api/job-board-api")
            .build();

    public List<JobMatchDto> findMatches() {
        Resume resume = resumes.findTopByUserIdOrderByUploadedAtDesc(currentUser.require().getId())
                .orElseThrow(() -> new IllegalStateException("Upload a resume before viewing job matches"));

        JsonNode root;
        try {
            root = jobsClient.get().retrieve().body(JsonNode.class);
        } catch (RestClientException exception) {
            return List.of();
        }
        if (root == null || !root.path("data").isArray()) {
            return List.of();
        }

        Set<String> resumeWords = words(resume.getRawText());
        List<JobMatchDto> matches = new ArrayList<>();
        for (JsonNode job : root.path("data")) {
            String title = text(job, "title");
            String company = text(job, "company_name");
            String location = text(job, "location");
            String description = text(job, "description");
            String applyUrl = text(job, "url");
            if (title.isBlank() || applyUrl.isBlank()) {
                continue;
            }
            Set<String> jobWords = words(title + " " + description);
            long overlap = jobWords.stream().filter(resumeWords::contains).count();
            int score = Math.min(98, Math.max(25, 25 + (int) Math.round(73.0 * overlap / Math.max(1, Math.min(12, jobWords.size())))));
            matches.add(new JobMatchDto(title, company, location, clean(description), applyUrl, score, job.path("remote").asBoolean(false)));
        }
        return matches.stream()
                .sorted(Comparator.comparingInt(JobMatchDto::matchPercentage).reversed())
                .limit(20)
                .toList();
    }

    private Set<String> words(String value) {
        Set<String> result = new HashSet<>();
        var matcher = WORDS.matcher(value.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String word = matcher.group();
            if (!STOP_WORDS.contains(word)) {
                result.add(word);
            }
        }
        return result;
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("").trim();
    }

    private String clean(String description) {
        return description.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }
}
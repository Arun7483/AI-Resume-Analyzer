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
import org.springframework.beans.factory.annotation.Value;

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

        @Value("${app.jobs.feed-url:https://www.arbeitnow.com/api/job-board-api}")
        private String jobsFeedUrl;

    public List<JobMatchDto> findMatches() {
        Resume resume = resumes.findTopByUserIdOrderByUploadedAtDesc(currentUser.require().getId())
                .orElse(null);
        Set<String> resumeWords = resume == null ? Set.of() : words(resume.getRawText());

        JsonNode root;
        try {
            root = RestClient.builder().build().get().uri(jobsFeedUrl).retrieve().body(JsonNode.class);
        } catch (RestClientException exception) {
            return fallbackJobs(resumeWords);
        }
        if (root == null || !root.path("data").isArray()) {
            return fallbackJobs(resumeWords);
        }

        List<JobMatchDto> matches = new ArrayList<>();
        for (JsonNode job : root.path("data")) {
            try {
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
            } catch (RuntimeException ignored) {
                // Skip malformed third-party listings and keep the other jobs usable.
            }
        }
        List<JobMatchDto> ranked = matches.stream()
                .sorted(Comparator.comparingInt(JobMatchDto::matchPercentage).reversed())
                .limit(20)
                .toList();
        return ranked.isEmpty() ? fallbackJobs(resumeWords) : ranked;
    }

    private List<JobMatchDto> fallbackJobs(Set<String> resumeWords) {
        return List.of(
                fallback("Software Engineer", "Search active software engineering roles", "Remote", "https://www.linkedin.com/jobs/search/?keywords=software%20engineer", resumeWords),
                fallback("Frontend Developer", "Search active frontend developer roles", "Remote", "https://www.linkedin.com/jobs/search/?keywords=frontend%20developer", resumeWords),
                fallback("Backend Developer", "Search active backend developer roles", "Remote", "https://www.linkedin.com/jobs/search/?keywords=backend%20developer", resumeWords),
                fallback("Data Analyst", "Search active data analyst roles", "Remote", "https://www.linkedin.com/jobs/search/?keywords=data%20analyst", resumeWords),
                fallback("Product Designer", "Search active product designer roles", "Remote", "https://www.linkedin.com/jobs/search/?keywords=product%20designer", resumeWords)
        );
    }

    private JobMatchDto fallback(String title, String company, String location, String url, Set<String> resumeWords) {
        int score = resumeWords.isEmpty() ? 50 : 60;
        return new JobMatchDto(title, company, location, "Open the active search results and apply manually on the original job platform.", url, score, true);
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
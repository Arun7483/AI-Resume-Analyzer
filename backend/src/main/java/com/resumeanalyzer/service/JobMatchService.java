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
import java.util.Map;
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
        String resumeText = resume == null || resume.getRawText() == null ? "" : resume.getRawText();
        Set<String> resumeWords = words(resumeText);

        List<JobMatchDto> matches = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        for (int page = 1; page <= 25 && matches.size() < 1200; page++) {
            JsonNode root;
            try {
                String separator = jobsFeedUrl.contains("?") ? "&" : "?";
                root = RestClient.builder().build().get()
                        .uri(page == 1 ? jobsFeedUrl : jobsFeedUrl + separator + "page=" + page)
                        .retrieve().body(JsonNode.class);
            } catch (RestClientException exception) {
                if (page == 1) {
                    return fallbackJobs(resumeText);
                }
                break;
            }
            if (root == null || !root.path("data").isArray() || root.path("data").isEmpty()) {
                break;
            }

            for (JsonNode job : root.path("data")) {
                try {
                    String title = text(job, "title");
                    String company = text(job, "company_name");
                    String location = text(job, "location");
                    String description = text(job, "description");
                    String applyUrl = text(job, "url");
                    if (title.isBlank() || applyUrl.isBlank() || !seenUrls.add(applyUrl)) {
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
        }
        List<JobMatchDto> ranked = matches.stream()
                .sorted(Comparator.comparingInt(JobMatchDto::matchPercentage).reversed())
                .limit(1200)
                .toList();
        return ranked.isEmpty() ? fallbackJobs(resumeText) : ranked;
    }

    public List<JobMatchDto> fallbackMatches() {
        try {
            Resume resume = resumes.findTopByUserIdOrderByUploadedAtDesc(currentUser.require().getId()).orElse(null);
            return fallbackJobs(resume == null || resume.getRawText() == null ? "" : resume.getRawText());
        } catch (RuntimeException exception) {
            return fallbackJobs("");
        }
    }

    private List<JobMatchDto> fallbackJobs(String resumeText) {
        String normalized = resumeText.toLowerCase(Locale.ROOT);
        List<String> roles = detectRoles(normalized);
        List<String> seniority = detectSeniority(normalized);
        List<JobMatchDto> jobs = new ArrayList<>();
        int roleIndex = 0;

        for (String role : roles) {
            for (String level : seniority) {
                String title = level + " " + role;
                String query = java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8);
                Map<String, String> portals = Map.of(
                        "LinkedIn", "https://www.linkedin.com/jobs/search/?keywords=" + query,
                        "Naukri", "https://www.naukri.com/" + title.toLowerCase(Locale.ROOT).replace(' ', '-') + "-jobs",
                        "Foundit", "https://www.foundit.in/srp/results?query=" + query,
                        "Internshala", "https://internshala.com/jobs/keywords-" + title.toLowerCase(Locale.ROOT).replace(' ', '-') + "/"
                );
                for (Map.Entry<String, String> portal : portals.entrySet()) {
                    jobs.add(fallback(title, portal.getKey() + " active job search", "Current listings", portal.getValue(), 72 - Math.min(20, roleIndex * 4)));
                }
                roleIndex++;
            }
        }
        return jobs;
    }

    private List<String> detectRoles(String resumeText) {
        if (containsAny(resumeText, "electronics", "embedded", "microcontroller", "pcb", "fpga", "verilog", "vlsi", "circuit", "firmware", "arduino", "instrumentation")) {
            return List.of("Electronics Engineer", "Embedded Systems Engineer", "Hardware Design Engineer", "PCB Design Engineer", "Firmware Engineer", "Test and Validation Engineer");
        }
        if (containsAny(resumeText, "data analyst", "sql", "tableau", "power bi", "statistics", "analytics")) {
            return List.of("Data Analyst", "Business Analyst", "Data Engineer", "BI Analyst", "Reporting Analyst");
        }
        if (containsAny(resumeText, "figma", "user experience", "ux", "ui design", "prototype")) {
            return List.of("UX Designer", "UI Designer", "Product Designer", "UX Researcher");
        }
        if (containsAny(resumeText, "product manager", "roadmap", "agile", "scrum", "stakeholder")) {
            return List.of("Product Manager", "Product Analyst", "Program Manager", "Business Analyst");
        }
        return List.of("Software Engineer", "Frontend Developer", "Backend Developer", "QA Engineer", "DevOps Engineer");
    }

    private List<String> detectSeniority(String resumeText) {
        java.util.regex.Matcher years = Pattern.compile("(\\d+)\\+?\\s*(?:years|yrs)").matcher(resumeText);
        int maximumYears = 0;
        while (years.find()) {
            maximumYears = Math.max(maximumYears, Integer.parseInt(years.group(1)));
        }
        if (maximumYears == 0 && containsAny(resumeText, "fresher", "entry level", "intern", "graduate", "student")) {
            return List.of("Intern", "Graduate", "Junior");
        }
        if (maximumYears >= 6) {
            return List.of("Senior", "Lead", "Principal");
        }
        if (maximumYears >= 2) {
            return List.of("Mid-level", "Senior", "Specialist");
        }
        return List.of("Junior", "Associate", "Trainee");
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private JobMatchDto fallback(String title, String company, String location, String url, int score) {
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
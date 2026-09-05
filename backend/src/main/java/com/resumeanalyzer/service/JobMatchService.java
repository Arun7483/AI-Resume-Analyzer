package com.resumeanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.dto.JobMatchDto;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
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
        collectArbeitnow(matches, seenUrls, resumeWords);
        collectRemoteOk(matches, seenUrls, resumeWords);
        collectRemotive(matches, seenUrls, resumeWords);
        List<JobMatchDto> ranked = matches.stream()
            .sorted(Comparator.comparingInt(JobMatchDto::matchPercentage).reversed())
            .limit(1200)
            .toList();
        return ranked.isEmpty() ? fallbackJobs(resumeText) : ranked;
        }

        private void collectArbeitnow(List<JobMatchDto> matches, Set<String> seenUrls, Set<String> resumeWords) {
        Set<String> seenPages = new HashSet<>();
        for (int page = 1; page <= 100 && matches.size() < 1200; page++) {
            try {
                String separator = jobsFeedUrl.contains("?") ? "&" : "?";
                    JsonNode root = RestClient.builder().build().get()
                        .uri(page == 1 ? jobsFeedUrl : jobsFeedUrl + separator + "page=" + page)
                        .retrieve().body(JsonNode.class);
                    if (root == null || !root.path("data").isArray() || root.path("data").isEmpty()) {
                        break;
                    }
                    String pageSignature = root.path("data").get(0).path("url").asText("") + ":" + root.path("data").size();
                    if (!seenPages.add(pageSignature)) {
                        break;
                    }
                    for (JsonNode job : root.path("data")) {
                        addArbeitnowJob(matches, seenUrls, resumeWords, job);
                    }
                } catch (RuntimeException exception) {
                    log.warn("Arbeitnow jobs feed failed on page {} using {}: {}", page, jobsFeedUrl, exception.getMessage());
                if (page == 1) {
                    return;
                }
                break;
            }
            }
        }

        private void addArbeitnowJob(List<JobMatchDto> matches, Set<String> seenUrls, Set<String> resumeWords, JsonNode job) {
            try {
                String title = text(job, "title");
                String company = text(job, "company_name");
                String location = text(job, "location");
                String description = text(job, "description");
                String applyUrl = text(job, "url");
                if (title.isBlank() || applyUrl.isBlank() || !seenUrls.add(applyUrl)) {
                    return;
            }
                matches.add(toMatch(title, company, location, description, applyUrl, job.path("remote").asBoolean(false), resumeWords));
            } catch (RuntimeException ignored) {
                // Skip malformed third-party listings and keep the other jobs usable.
        }
    }

    private void collectRemoteOk(List<JobMatchDto> matches, Set<String> seenUrls, Set<String> resumeWords) {
        try {
            JsonNode root = RestClient.builder().build().get().uri("https://remoteok.com/api")
                    .header("User-Agent", "ResumePulse/1.0").retrieve().body(JsonNode.class);
            if (root == null || !root.isArray()) return;
            for (JsonNode job : root) {
                String url = text(job, "url");
                if (url.isBlank()) url = "https://remoteok.com/remote-jobs/" + text(job, "slug");
                if (text(job, "position").isBlank() || !seenUrls.add(url)) continue;
                matches.add(toMatch(text(job, "position"), text(job, "company"), text(job, "location"), text(job, "description"), url, true, resumeWords));
            }
        } catch (RestClientException ignored) {
            log.warn("Remote OK jobs feed unavailable: {}", ignored.getMessage());
            // Arbeitnow and the resume-based fallback remain available when this feed is unavailable.
        }
    }

    private void collectRemotive(List<JobMatchDto> matches, Set<String> seenUrls, Set<String> resumeWords) {
        try {
            JsonNode root = RestClient.builder().build().get().uri("https://remotive.com/api/remote-jobs?limit=100")
                    .retrieve().body(JsonNode.class);
            if (root == null || !root.path("jobs").isArray()) return;
            for (JsonNode job : root.path("jobs")) {
                String url = text(job, "url");
                if (text(job, "title").isBlank() || url.isBlank() || !seenUrls.add(url)) continue;
                matches.add(toMatch(text(job, "title"), text(job, "company_name"), text(job, "candidate_required_location"), text(job, "description"), url, true, resumeWords));
            }
        } catch (RestClientException ignored) {
            log.warn("Remotive jobs feed unavailable: {}", ignored.getMessage());
            // Keep the primary feed results when this feed is unavailable.
        }
    }

    private JobMatchDto toMatch(String title, String company, String location, String description, String url, boolean remote, Set<String> resumeWords) {
        Set<String> jobWords = words(title + " " + description);
        long overlap = jobWords.stream().filter(resumeWords::contains).count();
        int score = Math.min(98, Math.max(25, 25 + (int) Math.round(73.0 * overlap / Math.max(1, Math.min(12, jobWords.size())))));
        return new JobMatchDto(title, company, location, clean(description), url, score, remote);
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
                    "Internshala", "https://internshala.com/jobs/keywords-" + title.toLowerCase(Locale.ROOT).replace(' ', '-') + "/",
                    "Unstop", "https://unstop.com/jobs?search=" + query
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
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        boolean hardwareProfile = containsAny(resumeText, "electronics", "electronic", "embedded", "microcontroller", "micro controller", "pcb", "fpga", "verilog", "vhdl", "vlsi", "circuit", "firmware", "arduino", "raspberry pi", "instrumentation", "altium", "proteus", "ltspice", "cadence", "hardware", "iot");
        if (hardwareProfile) {
            roles.addAll(List.of("Electronics Engineer", "Embedded Systems Engineer", "Hardware Design Engineer", "PCB Design Engineer", "Firmware Engineer", "FPGA Engineer"));
        }
        if (!hardwareProfile && containsAny(resumeText, "java", "spring boot", "python", "javascript", "typescript", "react", "angular", "node.js", "software engineer", "software developer", "rest api", "docker", "kubernetes")) {
            roles.addAll(List.of("Software Engineer", "Backend Developer", "Frontend Developer", "Full Stack Developer", "DevOps Engineer"));
        }
        if (containsAny(resumeText, "data analyst", "sql", "tableau", "power bi", "statistics", "analytics", "machine learning", "data science", "pandas", "tensorflow")) {
            roles.addAll(List.of("Data Analyst", "Data Scientist", "Data Engineer", "Business Intelligence Analyst", "Machine Learning Engineer"));
        }
        if (containsAny(resumeText, "cybersecurity", "information security", "soc analyst", "penetration testing", "ethical hacker", "siem", "vulnerability", "incident response")) {
            roles.addAll(List.of("Cybersecurity Analyst", "SOC Analyst", "Security Engineer", "Penetration Tester", "Cloud Security Engineer"));
        }
        if (containsAny(resumeText, "mechanical engineer", "solidworks", "autocad", "ansys", "manufacturing", "production engineer", "cnc", "cad", "cam", "thermodynamics", "hvac")) {
            roles.addAll(List.of("Mechanical Engineer", "Mechanical Design Engineer", "Manufacturing Engineer", "Production Engineer", "CAD Engineer"));
        }
        if (containsAny(resumeText, "civil engineer", "structural", "construction", "autocad civil", "quantity survey", "geotechnical", "site engineer", "revit")) {
            roles.addAll(List.of("Civil Engineer", "Structural Engineer", "Construction Engineer", "Site Engineer", "Project Engineer"));
        }
        if (containsAny(resumeText, "automotive", "vehicle", "ev engineer", "battery management", "adas", "autonomous driving", "powertrain")) {
            roles.addAll(List.of("Automotive Engineer", "Automotive Embedded Engineer", "EV Engineer", "Battery Engineer", "ADAS Engineer"));
        }
        if (containsAny(resumeText, "figma", "user experience", "ux", "ui design", "prototype", "wireframe", "graphic design")) {
            roles.addAll(List.of("UX Designer", "UI Designer", "Product Designer", "UX Researcher", "Visual Designer"));
        }
        if (containsAny(resumeText, "product manager", "roadmap", "agile", "scrum", "stakeholder", "product owner")) {
            roles.addAll(List.of("Product Manager", "Product Owner", "Program Manager", "Business Analyst", "Project Manager"));
        }
        if (containsAny(resumeText, "marketing", "seo", "sem", "social media", "content marketing", "brand manager")) {
            roles.addAll(List.of("Digital Marketing Specialist", "SEO Specialist", "Marketing Analyst", "Content Strategist", "Brand Manager"));
        }
        if (containsAny(resumeText, "accounting", "finance", "financial analyst", "banking", "audit", "tax", "risk management")) {
            roles.addAll(List.of("Financial Analyst", "Accountant", "Risk Analyst", "Audit Analyst", "Banking Operations Analyst"));
        }
        if (containsAny(resumeText, "nurse", "clinical", "pharmac", "medical", "healthcare", "hospital", "biomedical")) {
            roles.addAll(List.of("Clinical Research Associate", "Healthcare Administrator", "Medical Laboratory Technician", "Biomedical Engineer", "Pharmacist"));
        }
        if (roles.isEmpty()) {
            roles.addAll(List.of("Software Engineer", "Business Analyst", "Operations Analyst", "Project Coordinator"));
        }
        return List.copyOf(roles);
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
        return new JobMatchDto(title, company, location, "Search current openings for this resume-matched role and apply on the original job platform.", url, score, true);
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
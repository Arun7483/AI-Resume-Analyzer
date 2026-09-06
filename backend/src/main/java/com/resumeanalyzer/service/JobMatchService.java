package com.resumeanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.resumeanalyzer.dto.JobMatchDto;
import com.resumeanalyzer.dto.JobMatchPageDto;
import com.resumeanalyzer.entity.Resume;
import com.resumeanalyzer.exception.BadRequestException;
import com.resumeanalyzer.exception.JobFeedUnavailableException;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.*;
import java.time.Instant;
import java.util.regex.Pattern;

/** Retrieves and ranks only individual listings supplied by Adzuna. */
@Service @RequiredArgsConstructor @Slf4j
public class JobMatchService {
    private static final Pattern WORD = Pattern.compile("[a-zA-Z][a-zA-Z+#.-]{1,}");
    private static final Set<String> STOP = Set.of("and","the","for","with","from","this","that","your","work","years","team","will","have","using","experience","engineer");
    private static final long CACHE_TTL_MS = 300_000L;
    private final ResumeRepository resumes;
    private final CurrentUser currentUser;
    private final ChatClient.Builder chatClientBuilder;
    private final Map<Long, CachedMatches> cache = new HashMap<>();
    @Value("${app.jobs.adzuna-app-id:}") private String adzunaAppId;
    @Value("${app.jobs.adzuna-app-key:}") private String adzunaAppKey;
    @Value("${app.jobs.adzuna-country:in}") private String adzunaCountry;
    @Value("${app.jobs.adzuna-max-pages-per-query:3}") private int maxPagesPerQuery;
    @Value("${app.jobs.adzuna-max-roles-per-query:6}") private int maxRolesPerQuery;
    @Value("${app.jobs.jooble-api-key:}") private String joobleApiKey;
    @Value("${app.jobs.jooble-max-roles-per-query:3}") private int joobleMaxRolesPerQuery;
    @Value("${app.jobs.jooble-location:India}") private String joobleLocation;
    @Value("${app.jobs.jobicy-base-url:https://jobicy.com/api/v2/remote-jobs}") private String jobicyBaseUrl;
    @Value("${spring.ai.openai.api-key:}") private String groqApiKey;

    public synchronized JobMatchPageDto findMatches(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new BadRequestException("page must be zero or greater and size must be between 1 and 100");
        Resume resume = resumes.findTopByUserIdOrderByUploadedAtDesc(currentUser.require().getId()).orElseThrow(() -> new BadRequestException("Upload a resume before requesting job matches"));
        CachedMatches cached = cache.get(resume.getId());
        if (cached == null || cached.createdAt + CACHE_TTL_MS < System.currentTimeMillis()) {
            cached = new CachedMatches(loadMatches(resume), System.currentTimeMillis()); cache.put(resume.getId(), cached);
        }
        int from = Math.min(page * size, cached.matches.size()), to = Math.min(from + size, cached.matches.size());
        return new JobMatchPageDto(cached.matches.subList(from, to), page, size, cached.matches.size(), to < cached.matches.size());
    }

    private List<JobMatchDto> loadMatches(Resume resume) {
        CandidateProfile profile = enrichWithGroq(resume.getRawText(), CandidateProfile.from(resume.getRawText()));
        if (profile.roles.isEmpty()) throw new BadRequestException("We could not identify a job domain from this resume yet");
        Map<String, JobMatchDto> unique = new LinkedHashMap<>();
        Set<String> dedupeKeys = new HashSet<>();
        int successfulProviders = 0;
        if (hasAdzunaCredentials()) {
            try { fetchAdzuna(unique, dedupeKeys, profile); successfulProviders++; }
            catch (RuntimeException exception) { log.warn("Adzuna feed failed for country {}: {}", adzunaCountry, exception.getMessage()); }
        } else log.warn("Adzuna feed is not configured");
        if (joobleApiKey != null && !joobleApiKey.isBlank()) {
            try { fetchJooble(unique, dedupeKeys, profile); successfulProviders++; }
            catch (RuntimeException exception) { log.warn("Jooble feed failed: {}", exception.getMessage()); }
        } else log.warn("Jooble feed is not configured");
        try { fetchJobicy(unique, dedupeKeys, profile); successfulProviders++; }
        catch (RuntimeException exception) { log.warn("Jobicy feed failed: {}", exception.getMessage()); }
        if (successfulProviders == 0) throw new JobFeedUnavailableException("All live job feeds are unavailable or not configured. Configure Adzuna, Jooble, or Jobicy and try again.");
        return unique.values().stream().sorted(Comparator.comparingInt(JobMatchDto::matchPercentage).thenComparing(this::freshness).reversed()).toList();
    }

    private boolean hasAdzunaCredentials() { return adzunaAppId != null && !adzunaAppId.isBlank() && adzunaAppKey != null && !adzunaAppKey.isBlank(); }

    private void fetchAdzuna(Map<String, JobMatchDto> unique, Set<String> dedupeKeys, CandidateProfile profile) {
        RestClient client = RestClient.builder().baseUrl("https://api.adzuna.com/v1/api").defaultHeader("Accept", "application/json").build();
        final String appId = adzunaAppId, appKey = adzunaAppKey, country = adzunaCountry;
        for (String role : profile.roles.stream().limit(Math.max(1, maxRolesPerQuery)).toList()) for (int providerPage = 1; providerPage <= Math.max(1, maxPagesPerQuery); providerPage++) {
            final String finalRole = role; final int finalPage = providerPage;
            JsonNode root = client.get().uri(b -> b.path("/jobs/{country}/search/{page}")
                            .queryParam("app_id", appId).queryParam("app_key", appKey).queryParam("what", finalRole)
                            .queryParam("results_per_page", 20).queryParam("content-type", "application/json").build(country, finalPage))
                    .retrieve().body(JsonNode.class);
            JsonNode data = root == null ? null : root.path("results");
            if (data == null || !data.isArray() || data.isEmpty()) break;
            for (JsonNode job : data) addAdzunaJob(unique, dedupeKeys, job, profile);
        }
    }

    private void fetchJooble(Map<String, JobMatchDto> unique, Set<String> dedupeKeys, CandidateProfile profile) {
        String baseUrl = "India".equalsIgnoreCase(joobleLocation) || "in".equalsIgnoreCase(adzunaCountry) ? "https://in.jooble.org" : "https://jooble.org";
        RestClient client = RestClient.builder().baseUrl(baseUrl).defaultHeader("Accept", "application/json").build();
        for (String role : profile.roles.stream().limit(Math.max(1, joobleMaxRolesPerQuery)).toList()) {
            Map<String, Object> request = Map.of("keywords", role, "location", joobleLocation, "page", 1, "ResultOnPage", 20);
            JsonNode root = client.post().uri("/api/" + joobleApiKey).body(request).retrieve().body(JsonNode.class);
            JsonNode jobs = root == null ? null : root.path("jobs");
            if (jobs == null || !jobs.isArray()) continue;
            for (JsonNode job : jobs) addJoobleJob(unique, dedupeKeys, job, profile);
        }
    }

    private void fetchJobicy(Map<String, JobMatchDto> unique, Set<String> dedupeKeys, CandidateProfile profile) {
        RestClient client = RestClient.builder().build();
        String geo = "in".equalsIgnoreCase(adzunaCountry) ? "india" : "";
        Set<String> tags = profile.roles.stream().limit(Math.max(1, Math.min(3, joobleMaxRolesPerQuery))).map(this::jobicyTag).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        for (String tag : tags) {
            String uri = jobicyBaseUrl + "?count=50&tag=" + java.net.URLEncoder.encode(tag, java.nio.charset.StandardCharsets.UTF_8);
            if (!geo.isBlank()) uri += "&geo=" + geo;
            JsonNode root = client.get().uri(uri).retrieve().body(JsonNode.class);
            JsonNode jobs = root == null ? null : root.path("jobs");
            if (jobs == null || !jobs.isArray()) continue;
            for (JsonNode job : jobs) addJobicyJob(unique, dedupeKeys, job, profile);
        }
    }

    private String jobicyTag(String role) { return role.length() > 50 ? role.substring(0, 50) : role; }

    private void addAdzunaJob(Map<String, JobMatchDto> unique, Set<String> dedupeKeys, JsonNode job, CandidateProfile profile) {
        String title = text(job,"title"), applyUrl = text(job,"redirect_url"), jobId = text(job,"id");
        if (title.isBlank() || !validUrl(applyUrl)) return;
        String company = text(job.path("company"),"display_name"), location = text(job.path("location"),"display_name");
        String key = !jobId.isBlank() ? "ADZUNA:id:" + jobId : "ADZUNA:url:" + normalUrl(applyUrl), fallback = "fallback:" + normal(title) + "|" + normal(company) + "|" + normal(location);
        if (isDuplicate(dedupeKeys, key, "url:" + normalUrl(applyUrl), fallback)) return;
        String description = clean(text(job,"description")); if (description.isBlank()) return; MatchDetails match = score(profile, title, description, location, false);
        JobMatchDto listing = new JobMatchDto(jobId,"Adzuna",title,company,location,"",description,applyUrl,"Adzuna",text(job.path("category"),"label"),parseDate(text(job,"created")),contract(job),number(job,"salary_min"),number(job,"salary_max"),"",false,match.score,match.matched,match.missing);
        putJob(unique, dedupeKeys, key, "url:" + normalUrl(applyUrl), fallback, listing);
    }

    private void addJoobleJob(Map<String, JobMatchDto> unique, Set<String> dedupeKeys, JsonNode job, CandidateProfile profile) {
        String title = text(job, "title"), company = text(job, "company"), location = text(job, "location"), description = clean(text(job, "snippet")), applyUrl = text(job, "link"), jobId = text(job, "id");
        if (title.isBlank() || description.isBlank() || !validUrl(applyUrl)) return;
        String key = !jobId.isBlank() ? "JOOBLE:id:" + jobId : "JOOBLE:url:" + normalUrl(applyUrl);
        String fallback = "fallback:" + normal(title) + "|" + normal(company) + "|" + normal(location);
        if (isDuplicate(dedupeKeys, key, "url:" + normalUrl(applyUrl), fallback)) return;
        MatchDetails match = score(profile, title, description, location, false);
        JobMatchDto listing = new JobMatchDto(jobId, "Jooble", title, company, location, "", description, applyUrl, text(job, "source"), "", parseDate(text(job, "updated")), text(job, "type"), null, null, "", false, match.score, match.matched, match.missing);
        putJob(unique, dedupeKeys, key, "url:" + normalUrl(applyUrl), fallback, listing);
    }

    private void addJobicyJob(Map<String, JobMatchDto> unique, Set<String> dedupeKeys, JsonNode job, CandidateProfile profile) {
        String title = text(job, "jobTitle"), company = text(job, "companyName"), location = text(job, "jobGeo"), applyUrl = text(job, "url"), jobId = text(job, "id");
        String description = clean(text(job, "jobDescription")); if (description.isBlank()) description = clean(text(job, "jobExcerpt"));
        if (title.isBlank() || description.isBlank() || !validUrl(applyUrl)) return;
        String key = !jobId.isBlank() ? "JOBICY:id:" + jobId : "JOBICY:url:" + normalUrl(applyUrl);
        String fallback = "fallback:" + normal(title) + "|" + normal(company) + "|" + normal(location);
        if (isDuplicate(dedupeKeys, key, "url:" + normalUrl(applyUrl), fallback)) return;
        MatchDetails match = score(profile, title, description, location, true);
        JobMatchDto listing = new JobMatchDto(jobId, "Jobicy", title, company, location, "", description, applyUrl, "Jobicy", arrayText(job, "jobIndustry"), parseDate(text(job, "pubDate")), arrayText(job, "jobType"), number(job, "salaryMin"), number(job, "salaryMax"), text(job, "salaryCurrency"), true, match.score, match.matched, match.missing);
        putJob(unique, dedupeKeys, key, "url:" + normalUrl(applyUrl), fallback, listing);
    }

    private void putJob(Map<String, JobMatchDto> unique, Set<String> dedupeKeys, String primary, String urlKey, String fallback, JobMatchDto job) {
        unique.put(primary, job); dedupeKeys.add(primary); dedupeKeys.add(urlKey); dedupeKeys.add(fallback);
    }
    private boolean isDuplicate(Set<String> keys, String primary, String urlKey, String fallback) { return keys.contains(primary) || keys.contains(urlKey) || keys.contains(fallback); }
    private boolean validUrl(String value) { try { java.net.URI uri = java.net.URI.create(value); return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null; } catch (IllegalArgumentException exception) { return false; } }
    private String arrayText(JsonNode node, String field) { JsonNode value = node.path(field); if (value.isArray()) { List<String> values = new ArrayList<>(); value.forEach(item -> values.add(item.asText(""))); return String.join(", ", values); } return value.asText(""); }
    private Instant freshness(JobMatchDto job) { try { return job.postedAt() == null || job.postedAt().isBlank() ? Instant.MIN : Instant.parse(job.postedAt()); } catch (RuntimeException exception) { return Instant.MIN; } }

    private String parseDate(String value) {
        if (value == null || value.isBlank()) return "";
        try { return Instant.parse(value).toString(); } catch (Exception e1) {
            try { return java.time.LocalDateTime.parse(value.replace(" ", "T")).atZone(java.time.ZoneId.systemDefault()).toInstant().toString(); } catch (Exception e2) {
                try { return java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.parse(value, Instant::from).toString(); } catch (Exception e3) { return ""; }
            }
        }
    }

    private MatchDetails score(CandidateProfile profile, String title, String description, String location, boolean remote) {
        Set<String> jobWords = words(title + " " + description); long matched = profile.skills.stream().filter(jobWords::contains).count();
        List<String> present = profile.skills.stream().filter(jobWords::contains).limit(12).toList(), missing = profile.skills.stream().filter(s -> !jobWords.contains(s)).limit(12).toList();
        boolean role = profile.roles.stream().anyMatch(r -> normal(title).contains(normal(r)) || overlap(words(r), words(title)) >= 1);
        boolean domain = profile.domain.stream().anyMatch(term -> jobWords.contains(term) || normal(title).contains(term));
        int roleScore = role ? 35 : domain ? 22 : 0, skillScore = profile.skills.isEmpty() ? 0 : (int)Math.round(30d * matched / profile.skills.size());
        int seniority = seniorityCompatible(profile.seniority, title + " " + description) ? 15 : 5;
        int locationScore = remote || (!profile.location.isBlank() && normal(location).contains(normal(profile.location))) ? 5 : 0;
        int educationScore = !profile.education.isBlank() && normal(description).contains(normal(profile.education)) ? 5 : 0;
        return new MatchDetails(Math.max(0,Math.min(100,roleScore + skillScore + seniority + educationScore + (domain ? 10 : 0) + locationScore)),present,missing);
    }
    private boolean seniorityCompatible(String seniority, String value) { value=value.toLowerCase(Locale.ROOT); return !"entry".equals(seniority) || !(value.contains("senior")||value.contains("lead")||value.contains("principal")); }
    private int overlap(Set<String> left, Set<String> right) { return (int)left.stream().filter(right::contains).count(); }
    private Set<String> words(String value) { Set<String> r=new LinkedHashSet<>(); var m=WORD.matcher(value.toLowerCase(Locale.ROOT)); while(m.find()) if(!STOP.contains(m.group())) r.add(m.group()); return r; }
    private String text(JsonNode node,String field) { return node.path(field).asText("").trim(); }
    private Double number(JsonNode node,String field) { return node.hasNonNull(field)&&node.path(field).isNumber()?node.path(field).asDouble():null; }
    private String clean(String value) { return value.replaceAll("<[^>]+>"," ").replaceAll("\\s+"," ").trim(); }
    private String join(String... values) { return Arrays.stream(values).filter(v->!v.isBlank()).distinct().reduce((a,b)->a+", "+b).orElse(""); }
    private String normal(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim(); }
    private String normalUrl(String value) { return value.trim().replaceAll("[?#].*$", "").replaceAll("/$", "").toLowerCase(Locale.ROOT); }
    private String contract(JsonNode job) { return join(text(job, "contract_type"), text(job, "contract_time")); }
    private CandidateProfile enrichWithGroq(String resume, CandidateProfile fallback) {
        if (groqApiKey == null || groqApiKey.isBlank()) return fallback;
        try {
            AiProfile ai = chatClientBuilder.build().prompt().system("Extract a factual candidate profile. Return only structured data; do not infer unrelated career domains.")
                    .user("Extract domain, roles, skills, seniority and location from this resume. Hardware/VLSI/FPGA/embedded candidates must retain those specialisms.\n\n" + resume).call().entity(AiProfile.class);
            if (ai == null || ai.roles == null || ai.roles.isEmpty()) return fallback;
            LinkedHashSet<String> roles = new LinkedHashSet<>(ai.roles); LinkedHashSet<String> skills = new LinkedHashSet<>(fallback.skills); if (ai.skills != null) ai.skills.stream().filter(Objects::nonNull).map(s -> s.toLowerCase(Locale.ROOT).trim()).filter(s -> !s.isBlank()).forEach(skills::add);
            LinkedHashSet<String> domain = new LinkedHashSet<>(fallback.domain); if (ai.domain != null && !ai.domain.isBlank()) domain.addAll(words(ai.domain));
            return new CandidateProfile(List.copyOf(roles), Set.copyOf(skills), Set.copyOf(domain), ai.seniority == null || ai.seniority.isBlank() ? fallback.seniority : ai.seniority.toLowerCase(Locale.ROOT), ai.location == null ? fallback.location : ai.location, fallback.education);
        } catch (RuntimeException exception) { log.info("Groq candidate-profile extraction unavailable; using deterministic profile: {}", exception.getMessage()); return fallback; }
    }
    private record CachedMatches(List<JobMatchDto> matches,long createdAt) {}
    private record MatchDetails(int score,List<String> matched,List<String> missing) {}
    private record AiProfile(String domain,List<String> roles,List<String> skills,String seniority,String location) {}
    private record CandidateProfile(List<String> roles,Set<String> skills,Set<String> domain,String seniority,String location,String education) {
        static CandidateProfile from(String raw) {
            String text=Objects.requireNonNullElse(raw,"").toLowerCase(Locale.ROOT); LinkedHashSet<String> roles=new LinkedHashSet<>(), skills=new LinkedHashSet<>(), domain=new LinkedHashSet<>();
            add(text,roles,skills,domain,List.of("vlsi","asic","semiconductor","verilog","systemverilog","rtl","physical design","sta","dft","cadence"),List.of("VLSI Design Engineer","ASIC Design Engineer","RTL Design Engineer","Design Verification Engineer","Physical Design Engineer","DFT Engineer","STA Engineer","SoC Verification Engineer","Silicon Validation Engineer"));
            add(text,roles,skills,domain,List.of("fpga","vhdl","digital logic"),List.of("FPGA Engineer","FPGA Design Engineer","FPGA Verification Engineer","RTL Engineer"));
            add(text,roles,skills,domain,List.of("embedded","firmware","microcontroller","embedded linux","arduino","raspberry pi","iot"),List.of("Embedded Systems Engineer","Embedded Firmware Engineer","Firmware Engineer","Embedded Linux Engineer","IoT Engineer","Microcontroller Engineer"));
            add(text,roles,skills,domain,List.of("electronics","pcb","circuit","board design","analog","instrumentation","altium","ltspice"),List.of("Electronics Engineer","Hardware Design Engineer","PCB Design Engineer","Circuit Design Engineer","Hardware Verification Engineer","Electronics Test Engineer"));
            add(text,roles,skills,domain,List.of("automotive","adas","battery management","electric vehicle","ev "),List.of("Automotive Embedded Engineer","Automotive Electronics Engineer","EV Engineer","Battery Management Engineer","ADAS Engineer"));
            add(text,roles,skills,domain,List.of("data analyst","data science","machine learning","tableau","power bi","pandas","tensorflow"),List.of("Data Analyst","Data Scientist","Data Engineer","Business Intelligence Analyst","Machine Learning Engineer"));
            add(text,roles,skills,domain,List.of("cybersecurity","information security","soc analyst","penetration testing","siem","vulnerability","incident response"),List.of("Cybersecurity Analyst","SOC Analyst","Security Engineer","Penetration Tester","Cloud Security Engineer"));
            add(text,roles,skills,domain,List.of("mechanical engineer","solidworks","manufacturing","production engineer","thermodynamics","hvac"),List.of("Mechanical Engineer","Mechanical Design Engineer","Manufacturing Engineer","Production Engineer","CAD Engineer"));
            add(text,roles,skills,domain,List.of("civil engineer","structural","construction","quantity survey","geotechnical","site engineer","revit"),List.of("Civil Engineer","Structural Engineer","Construction Engineer","Site Engineer","Project Engineer"));
            add(text,roles,skills,domain,List.of("figma","user experience","ux","ui design","prototype","wireframe"),List.of("UX Designer","UI Designer","Product Designer","UX Researcher"));
            add(text,roles,skills,domain,List.of("product manager","roadmap","agile","scrum","product owner"),List.of("Product Manager","Product Owner","Program Manager"));
            if(roles.isEmpty()&&has(text,"java","spring boot","angular","react","typescript","python")) roles.addAll(List.of("Software Engineer","Backend Developer","Frontend Developer"));
            for(String s:List.of("verilog","systemverilog","vhdl","cmos","cadence","fpga","rtl","embedded c","c++","pcb","altium","ltspice","linux","python","java","spring boot","angular","typescript")) if(text.contains(s)) skills.add(s);
            String education = has(text,"b.tech","btech","bachelor of technology") ? "bachelor" : has(text,"m.tech","master") ? "master" : "";
            return new CandidateProfile(List.copyOf(roles),Set.copyOf(skills),Set.copyOf(domain),has(text,"fresher","intern","student","graduate","entry level")?"entry":has(text,"senior","lead","6 years","7 years")?"senior":"mid","",education);
        }
        private static void add(String text,Set<String> roles,Set<String> skills,Set<String> domain,List<String> signals,List<String> matches) { if(has(text,signals.toArray(String[]::new))) {roles.addAll(matches);domain.addAll(signals);signals.stream().filter(text::contains).forEach(skills::add);} }
        private static boolean has(String text,String... terms) { for(String term:terms) if(text.contains(term)) return true; return false; }
    }
}

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
        if (adzunaAppId == null || adzunaAppId.isBlank() || adzunaAppKey == null || adzunaAppKey.isBlank()) throw new JobFeedUnavailableException("Live job feed is not configured. Set ADZUNA_APP_ID and ADZUNA_APP_KEY and try again.");
        CandidateProfile profile = enrichWithGroq(resume.getRawText(), CandidateProfile.from(resume.getRawText()));
        if (profile.roles.isEmpty()) throw new BadRequestException("We could not identify a job domain from this resume yet");
        RestClient client = RestClient.builder().baseUrl("https://api.adzuna.com/v1/api").defaultHeader("Accept", "application/json").build();
        Map<String, JobMatchDto> unique = new LinkedHashMap<>(); boolean responded = false;
        try {
            for (String role : profile.roles.stream().limit(Math.max(1, maxRolesPerQuery)).toList()) for (int providerPage = 1; providerPage <= Math.max(1, maxPagesPerQuery); providerPage++) {
                final int adzunaPage = providerPage;
                JsonNode root = client.get().uri(b -> b.path("/jobs/{country}/search/{page}")
                                .queryParam("app_id", adzunaAppId).queryParam("app_key", adzunaAppKey).queryParam("what", role)
                                .queryParam("results_per_page", 20).queryParam("content-type", "application/json").build(adzunaCountry, adzunaPage))
                        .retrieve().body(JsonNode.class);
                responded = true; JsonNode data = root == null ? null : root.path("results");
                if (data == null || !data.isArray() || data.isEmpty()) break;
                for (JsonNode job : data) addAdzunaJob(unique, job, profile);
            }
        } catch (RestClientException | IllegalStateException exception) {
            log.warn("Adzuna request failed for country {}: {}", adzunaCountry, exception.getMessage());
            throw new JobFeedUnavailableException("The live Adzuna feed is temporarily unavailable. Please try again later.");
        }
        if (!responded) throw new JobFeedUnavailableException("The live Adzuna feed returned no response. Please try again later.");
        return unique.values().stream().distinct().sorted(Comparator.comparingInt(JobMatchDto::matchPercentage).reversed()).toList();
    }

    private void addAdzunaJob(Map<String, JobMatchDto> unique, JsonNode job, CandidateProfile profile) {
        String title = text(job,"title"), applyUrl = text(job,"redirect_url"), jobId = text(job,"id");
        if (title.isBlank() || applyUrl.isBlank()) return;
        String company = text(job.path("company"),"display_name"), location = text(job.path("location"),"display_name");
        String key = !jobId.isBlank() ? "id:" + jobId : "url:" + normalUrl(applyUrl), fallback = "fallback:" + normal(title) + "|" + normal(company) + "|" + normal(location);
        if (unique.containsKey(key) || unique.containsKey("url:" + normalUrl(applyUrl)) || unique.containsKey(fallback)) return;
        String description = clean(text(job,"description")); MatchDetails match = score(profile, title, description, location, false);
        JobMatchDto listing = new JobMatchDto(jobId,title,company,location,adzunaCountry.toUpperCase(Locale.ROOT),description,applyUrl,"Adzuna",text(job.path("category"),"label"),text(job,"created"),contract(job),number(job,"salary_min"),number(job,"salary_max"),"",false,match.score,match.matched,match.missing);
        unique.put(key, listing); unique.put("url:" + normalUrl(applyUrl), listing); unique.put(fallback, listing);
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

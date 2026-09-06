package com.resumeanalyzer.controller;

import com.resumeanalyzer.dto.AnalysisResponseDto;
import com.resumeanalyzer.dto.JobMatchPageDto;
import com.resumeanalyzer.dto.ResumeHistoryDto;
import com.resumeanalyzer.repository.ResumeRepository;
import com.resumeanalyzer.security.CurrentUser;
import com.resumeanalyzer.service.JobMatchService;
import com.resumeanalyzer.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {
    private final ResumeService resumes;
    private final JobMatchService jobs;
    private final ResumeRepository resumeRepository;
    private final CurrentUser currentUser;
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    AnalysisResponseDto upload(@RequestPart("file") MultipartFile file, @RequestPart(value = "jobDescription", required = false) String jobDescription) { return resumes.uploadAndAnalyze(file, jobDescription); }
    @GetMapping("/job-matches")
    JobMatchPageDto jobMatches(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "24") int size) { return jobs.findMatches(page, size); }
    @Transactional(readOnly = true) @GetMapping("/history")
    List<ResumeHistoryDto> history() { return resumeRepository.findHistoryByUserId(currentUser.require().getId()); }
}

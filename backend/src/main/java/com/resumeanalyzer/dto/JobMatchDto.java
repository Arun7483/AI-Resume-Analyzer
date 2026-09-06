package com.resumeanalyzer.dto;

import java.util.List;

public record JobMatchDto(String jobId, String provider, String title, String company, String location, String country,
                          String description, String applyUrl, String publisher, String category, String postedAt,
                          String employmentType, Double salaryMin, Double salaryMax, String salaryCurrency,
                          boolean remote, int matchPercentage, List<String> matchedSkills, List<String> missingSkills) { }

package com.resumeanalyzer.dto;

public record JobMatchDto(
        String title,
        String company,
        String location,
        String description,
        String applyUrl,
        int matchPercentage,
        boolean remote
) {
}

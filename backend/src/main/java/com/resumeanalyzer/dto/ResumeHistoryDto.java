package com.resumeanalyzer.dto;

import java.time.Instant;

public record ResumeHistoryDto(Long id, String fileName, Instant uploadedAt) {
}
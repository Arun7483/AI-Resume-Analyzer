package com.resumeanalyzer.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(@NotBlank String credential) {
}
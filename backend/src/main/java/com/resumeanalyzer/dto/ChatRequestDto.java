package com.resumeanalyzer.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Size;
public record ChatRequestDto(@NotBlank @Size(max=4000) String prompt,Long resumeId) { }

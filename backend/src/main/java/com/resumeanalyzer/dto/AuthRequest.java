package com.resumeanalyzer.dto;
import jakarta.validation.constraints.*;
public record AuthRequest(@NotBlank @Email @Size(max=255) String email,@NotBlank @Size(min=8,max=72) String password,@Size(max=100) String fullName) { }

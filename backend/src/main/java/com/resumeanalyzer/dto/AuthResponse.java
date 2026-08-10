package com.resumeanalyzer.dto;
public record AuthResponse(String accessToken,String tokenType,long expiresIn,String fullName,String role) { }

package com.resumeanalyzer.dto;
import java.time.Instant;
public record ChatResponseDto(Long messageId,String content,Instant timestamp) { }

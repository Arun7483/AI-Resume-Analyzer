package com.resumeanalyzer.dto;
import java.time.Instant; import java.util.List;
public record AnalysisResponseDto(Long resumeId,String fileName,int overallScore,int atsMatchPercentage,List<String> strengths,List<String> weaknesses,List<String> missingKeywords,Instant analyzedAt) { }

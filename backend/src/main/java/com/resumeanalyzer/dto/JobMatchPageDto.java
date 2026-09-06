package com.resumeanalyzer.dto;

import java.util.List;

public record JobMatchPageDto(List<JobMatchDto> jobs, int page, int size, int total, boolean hasMore) { }

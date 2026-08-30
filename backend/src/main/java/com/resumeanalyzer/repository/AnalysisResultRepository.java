package com.resumeanalyzer.repository;
import com.resumeanalyzer.entity.AnalysisResult; import org.springframework.data.jpa.repository.JpaRepository;
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult,Long> { }

package com.resumeanalyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
        name = "analysis_results",
        indexes = {
                @Index(
                        name = "idx_analysis_resume",
                        columnList = "resume_id"
                ),
                @Index(
                        name = "idx_analysis_date",
                        columnList = "analyzed_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "resume_id",
            nullable = false
    )
    private Resume resume;

    @Column(
            name = "overall_score",
            nullable = false
    )
    private Integer overallScore;

    @Column(
            name = "ats_match_percentage",
            nullable = false
    )
    private Integer atsMatchPercentage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "strengths_json",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String strengthsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "weaknesses_json",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String weaknessesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "missing_keywords_json",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private String missingKeywordsJson;

    @CreationTimestamp
    @Column(
            name = "analyzed_at",
            nullable = false,
            updatable = false
    )
    private Instant analyzedAt;
}
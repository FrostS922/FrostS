package com.frosts.testplatform.dto.performancereport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisResponse {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ErrorSummary errorSummary;
    private PerformanceSummary performanceSummary;
    private List<String> correlations;
    private String conclusion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorSummary {
        private long totalErrors;
        private List<String> topCategories;
        private List<String> topMessages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceSummary {
        private long totalPoorMetrics;
        private List<MetricIssue> poorMetrics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricIssue {
        private String metricName;
        private Double avgValue;
        private Long poorCount;
        private Long totalCount;
    }
}

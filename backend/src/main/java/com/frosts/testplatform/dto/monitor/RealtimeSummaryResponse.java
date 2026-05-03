package com.frosts.testplatform.dto.monitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeSummaryResponse {

    private PerformanceSnapshot performance;
    private ErrorSnapshot errors;
    private SecuritySnapshot security;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceSnapshot {
        private long totalReports;
        private long todayReports;
        private List<MetricSnapshot> metrics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricSnapshot {
        private String metricName;
        private double avgValue;
        private long count;
        private long poorCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorSnapshot {
        private long totalErrors;
        private long todayErrors;
        private List<RecentError> recentErrors;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentError {
        private String errorMessage;
        private String category;
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecuritySnapshot {
        private long todayLoginSuccesses;
        private long todayLoginFailures;
        private long anomalousIps;
        private long bannedIps;
    }
}

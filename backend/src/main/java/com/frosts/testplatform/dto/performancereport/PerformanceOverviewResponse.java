package com.frosts.testplatform.dto.performancereport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceOverviewResponse {

    private long totalReports;
    private long todayReports;
    private List<MetricStat> metrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricStat {
        private String metricName;
        private Double avgValue;
        private Double minValue;
        private Double maxValue;
        private Long count;
        private Long poorCount;
    }
}

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
public class PerformancePercentileResponse {

    private List<MetricPercentile> metrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricPercentile {
        private String metricName;
        private Double p50;
        private Double p75;
        private Double p90;
        private Double p95;
        private Double p99;
        private Long sampleCount;
    }
}

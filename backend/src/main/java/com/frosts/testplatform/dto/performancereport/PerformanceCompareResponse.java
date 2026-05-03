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
public class PerformanceCompareResponse {

    private List<String> labels;
    private List<MetricCompare> metrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricCompare {
        private String metricName;
        private List<Double> avgValues;
        private List<Long> sampleCounts;
    }
}

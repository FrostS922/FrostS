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
public class PerformanceTrendResponse {

    private List<String> dates;
    private List<MetricTrendPoint> metrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricTrendPoint {
        private String metricName;
        private List<Double> values;
    }
}

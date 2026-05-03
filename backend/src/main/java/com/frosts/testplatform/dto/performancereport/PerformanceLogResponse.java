package com.frosts.testplatform.dto.performancereport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceLogResponse {

    private Long id;
    private String metricName;
    private Double metricValue;
    private String rating;
    private String pageUrl;
    private String userAgent;
    private String extraInfo;
    private String createdAt;
}

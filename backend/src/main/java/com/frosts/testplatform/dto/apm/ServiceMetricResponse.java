package com.frosts.testplatform.dto.apm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceMetricResponse {
    private String serviceName;
    private double avgResponseTime;
    private double p99;
    private double throughput;
    private double errorRate;
    private String healthStatus;
}

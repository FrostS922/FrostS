package com.frosts.testplatform.dto.performancereport;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class PerformanceReportRequest {

    @NotBlank(message = "指标名称不能为空")
    private String name;

    @NotNull(message = "指标值不能为空")
    private Double value;

    private String rating;

    private String url;

    private String userAgent;

    private Map<String, Object> extra;
}

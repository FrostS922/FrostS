package com.frosts.testplatform.dto.alertrule;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAlertRuleRequest {
    @NotBlank private String name;
    @NotBlank private String ruleType;
    private String metricName;
    private String conditionType;
    private Double threshold;
    private String comparator;
    private Integer windowMinutes;
    private Integer minSampleCount;
    private String notifyType;
    private String priority;
    private Integer cooldownMinutes;
    private String description;
}

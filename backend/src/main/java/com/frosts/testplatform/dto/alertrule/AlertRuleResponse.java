package com.frosts.testplatform.dto.alertrule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleResponse {
    private Long id;
    private String name;
    private String ruleType;
    private Boolean enabled;
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
    private String createdAt;
    private String updatedAt;
}

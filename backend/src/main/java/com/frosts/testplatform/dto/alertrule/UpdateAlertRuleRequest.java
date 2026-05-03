package com.frosts.testplatform.dto.alertrule;

import lombok.Data;

@Data
public class UpdateAlertRuleRequest {
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
}

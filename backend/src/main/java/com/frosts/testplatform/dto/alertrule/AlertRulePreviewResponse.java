package com.frosts.testplatform.dto.alertrule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRulePreviewResponse {
    private Double currentValue;
    private Double threshold;
    private Boolean wouldTrigger;
    private Long sampleCount;
    private String message;
}

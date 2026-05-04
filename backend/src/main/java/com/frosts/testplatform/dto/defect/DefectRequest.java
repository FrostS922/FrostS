package com.frosts.testplatform.dto.defect;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DefectRequest {
    @NotBlank(message = "缺陷标题不能为空")
    private String title;

    private Long projectId;

    private Long testCaseId;
    private Long testPlanCaseId;
    private String description;
    private String stepsToReproduce;
    private String expectedBehavior;
    private String actualBehavior;
    private String severity;
    private String priority;
    private String status;
    private String type;
    private String reportedBy;
    private String assignedTo;
    private String environment;
    private String foundInVersion;
    private String fixedInVersion;
    private String component;
    private String reproducibility;
    private String impact;
    private String workaround;
    private String rootCause;
    private Long duplicateOf;
    private String resolvedBy;
    private String verifiedBy;
    private String source;
}

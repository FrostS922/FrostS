package com.frosts.testplatform.dto.testcase;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestCaseRequest {
    @NotBlank(message = "用例标题不能为空")
    private String title;

    private Long projectId;

    private Long requirementId;
    private Long moduleId;
    private String description;
    private String preconditions;
    private String steps;
    private String testData;
    private String expectedResults;
    private String actualResults;
    private String postconditions;
    private String type;
    private String priority;
    private String status;
    private Integer executionTime;
    private Boolean isAutomated;
    private String automationScript;
    private String reviewer;
    private String reviewStatus;
    private String reviewComments;
    private String tags;
    private String version;
}

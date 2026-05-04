package com.frosts.testplatform.dto.testcase;

import java.time.LocalDateTime;

public record TestCaseResponse(
        Long id,
        String title,
        String caseNumber,
        Long projectId,
        String projectName,
        Long requirementId,
        String requirementTitle,
        Long moduleId,
        String moduleName,
        String description,
        String preconditions,
        String steps,
        String testData,
        String expectedResults,
        String actualResults,
        String postconditions,
        String type,
        String priority,
        String status,
        Integer executionTime,
        Boolean isAutomated,
        String automationScript,
        String reviewer,
        String reviewStatus,
        String reviewComments,
        String tags,
        String version,
        LocalDateTime lastExecutedAt,
        String lastExecutedBy,
        Integer passCount,
        Integer failCount,
        Integer totalExecutions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

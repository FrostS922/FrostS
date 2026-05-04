package com.frosts.testplatform.dto.requirement;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RequirementResponse(
        Long id,
        String title,
        String description,
        String acceptanceCriteria,
        String requirementNumber,
        Long projectId,
        String projectName,
        Long parentId,
        String parentTitle,
        String type,
        String priority,
        String status,
        String assignedTo,
        Integer storyPoints,
        Integer estimatedHours,
        Integer actualHours,
        LocalDate dueDate,
        LocalDate completedDate,
        String source,
        String rejectedReason,
        Long testCaseCount,
        Double coverageRate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

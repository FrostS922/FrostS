package com.frosts.testplatform.dto.defect;

import java.time.LocalDateTime;

public record DefectResponse(
        Long id,
        String title,
        String defectNumber,
        Long projectId,
        String projectName,
        Long testCaseId,
        String testCaseTitle,
        String description,
        String stepsToReproduce,
        String expectedBehavior,
        String actualBehavior,
        String severity,
        String priority,
        String status,
        String type,
        String reportedBy,
        String assignedTo,
        String resolvedBy,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        String verifiedBy,
        LocalDateTime verifiedAt,
        String environment,
        String foundInVersion,
        String fixedInVersion,
        String component,
        String reproducibility,
        String impact,
        String workaround,
        String rootCause,
        Long duplicateOf,
        Integer reopenCount,
        String source,
        String resolution,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

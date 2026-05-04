package com.frosts.testplatform.dto.testplan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TestPlanResponse(
        Long id,
        String name,
        String planNumber,
        Long projectId,
        String projectName,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate actualStartDate,
        LocalDate actualEndDate,
        String status,
        String owner,
        String environment,
        String milestone,
        BigDecimal progress,
        Integer totalCases,
        Integer passedCases,
        Integer failedCases,
        Integer blockedCases,
        Integer notRunCases,
        String risk,
        String entryCriteria,
        String exitCriteria,
        String testStrategy,
        String scope,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

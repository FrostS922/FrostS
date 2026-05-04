package com.frosts.testplatform.dto.project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectResponse(
        Long id,
        String name,
        String code,
        String description,
        String manager,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate actualEndDate,
        String status,
        String category,
        BigDecimal progress,
        Integer estimatedHours,
        Integer actualHours,
        String health,
        List<String> memberUsernames,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

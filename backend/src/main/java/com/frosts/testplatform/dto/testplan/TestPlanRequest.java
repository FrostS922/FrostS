package com.frosts.testplatform.dto.testplan;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TestPlanRequest {
    @NotBlank(message = "计划名称不能为空")
    private String name;

    private Long projectId;

    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private String status;
    private String owner;
    private String environment;
    private String milestone;
    private BigDecimal progress;
    private String risk;
    private String entryCriteria;
    private String exitCriteria;
    private String testStrategy;
    private String scope;
}

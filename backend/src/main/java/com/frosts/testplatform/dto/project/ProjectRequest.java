package com.frosts.testplatform.dto.project;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProjectRequest {
    @NotBlank(message = "项目名称不能为空")
    private String name;

    @NotBlank(message = "项目编码不能为空")
    private String code;

    private String description;
    private String manager;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate actualEndDate;
    private String status;
    private String category;
    private BigDecimal progress;
    private Integer estimatedHours;
    private Integer actualHours;
    private String health;
}

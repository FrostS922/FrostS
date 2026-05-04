package com.frosts.testplatform.dto.requirement;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RequirementRequest {
    @NotBlank(message = "需求标题不能为空")
    private String title;

    private Long projectId;

    private Long parentId;
    private String description;
    private String acceptanceCriteria;
    private String type;
    private String priority;
    private String status;
    private String assignedTo;
    private Integer storyPoints;
    private Integer estimatedHours;
    private Integer actualHours;
    private LocalDate dueDate;
    private String source;
    private String rejectedReason;
}

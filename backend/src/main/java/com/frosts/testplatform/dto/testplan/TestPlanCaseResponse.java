package com.frosts.testplatform.dto.testplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestPlanCaseResponse {

    private Long id;
    private Long testPlanId;
    private TestCaseSummary testCase;
    private String status;
    private String priority;
    private String assignedTo;
    private String actualResult;
    private String executedBy;
    private LocalDateTime executedAt;
    private String defectId;
    private String defectLink;
    private String evidence;
    private Integer retryCount;
    private Boolean isBlocked;
    private String blockReason;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}

package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "test_plan_case")
public class TestPlanCase extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_plan_id")
    private TestPlan testPlan;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id")
    private TestCase testCase;

    @Column(length = 20)
    private String status;

    @Column(length = 20)
    private String priority;

    @Column(length = 50)
    private String assignedTo;

    @Column(name = "actual_result", columnDefinition = "TEXT")
    private String actualResult;

    @Column(name = "executed_by", length = 50)
    private String executedBy;

    @Column(name = "executed_at")
    private java.time.LocalDateTime executedAt;

    @Column(name = "defect_id", length = 50)
    private String defectId;

    @Column(name = "defect_link", length = 500)
    private String defectLink;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "is_blocked")
    private Boolean isBlocked = false;

    @Column(name = "block_reason", columnDefinition = "TEXT")
    private String blockReason;

    private String comment;
}

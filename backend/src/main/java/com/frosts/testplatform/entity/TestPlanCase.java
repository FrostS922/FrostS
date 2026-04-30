package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "test_plan_case")
public class TestPlanCase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_plan_id")
    private TestPlan testPlan;

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

    private String comment;
}

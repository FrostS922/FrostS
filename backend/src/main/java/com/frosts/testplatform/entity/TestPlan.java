package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "test_plan")
public class TestPlan extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(unique = true, length = 50)
    private String planNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(length = 20)
    private String status;

    @Column(length = 50)
    private String owner;

    @Column(length = 100)
    private String environment;

    @Column(length = 100)
    private String milestone;

    @Column(precision = 5, scale = 2)
    private BigDecimal progress;

    @Column(name = "total_cases")
    private Integer totalCases = 0;

    @Column(name = "passed_cases")
    private Integer passedCases = 0;

    @Column(name = "failed_cases")
    private Integer failedCases = 0;

    @Column(name = "blocked_cases")
    private Integer blockedCases = 0;

    @Column(name = "not_run_cases")
    private Integer notRunCases = 0;

    @Column(columnDefinition = "TEXT")
    private String risk;

    @Column(name = "entry_criteria", columnDefinition = "TEXT")
    private String entryCriteria;

    @Column(name = "exit_criteria", columnDefinition = "TEXT")
    private String exitCriteria;

    @OneToMany(mappedBy = "testPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestPlanCase> testPlanCases;

    @Column(columnDefinition = "TEXT")
    private String testStrategy;

    @Column(columnDefinition = "TEXT")
    private String scope;
}

package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "test_case")
public class TestCase extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(unique = true, length = 50)
    private String caseNumber;

    private String description;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private TestCaseModule module;

    @Column(columnDefinition = "TEXT")
    private String preconditions;

    @Column(columnDefinition = "TEXT")
    private String steps;

    @Column(columnDefinition = "TEXT")
    private String testData;

    @Column(columnDefinition = "TEXT")
    private String expectedResults;

    @Column(columnDefinition = "TEXT")
    private String actualResults;

    @Column(columnDefinition = "TEXT")
    private String postconditions;

    @Column(length = 20)
    private String type;

    @Column(length = 20)
    private String priority;

    @Column(length = 20)
    private String status;

    @Column(name = "execution_time")
    private Integer executionTime;

    @Column(name = "is_automated")
    private Boolean isAutomated = false;

    @Column(name = "automation_script", length = 500)
    private String automationScript;

    @Column(length = 50)
    private String reviewer;

    @Column(name = "review_status", length = 20)
    private String reviewStatus;

    @Column(name = "review_comments", columnDefinition = "TEXT")
    private String reviewComments;

    @Column(length = 200)
    private String tags;

    @Column(length = 20)
    private String version = "1.0";

    @Column(name = "last_executed_at")
    private LocalDateTime lastExecutedAt;

    @Column(name = "last_executed_by", length = 50)
    private String lastExecutedBy;

    @Column(name = "pass_count")
    private Integer passCount = 0;

    @Column(name = "fail_count")
    private Integer failCount = 0;

    @Column(name = "total_executions")
    private Integer totalExecutions = 0;
}

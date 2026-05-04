package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "defect")
public class Defect extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(unique = true, length = 50)
    private String defectNumber;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id")
    private TestCase testCase;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_plan_case_id")
    private TestPlanCase testPlanCase;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String stepsToReproduce;

    @Column(columnDefinition = "TEXT")
    private String expectedBehavior;

    @Column(columnDefinition = "TEXT")
    private String actualBehavior;

    @Column(length = 20)
    private String severity;

    @Column(length = 20)
    private String priority;

    @Column(length = 20)
    private String status;

    @Column(length = 20)
    private String type;

    @Column(name = "reported_by", length = 50)
    private String reportedBy;

    @Column(name = "assigned_to", length = 50)
    private String assignedTo;

    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "verified_by", length = 50)
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(length = 100)
    private String environment;

    @Column(name = "found_in_version", length = 50)
    private String foundInVersion;

    @Column(name = "fixed_in_version", length = 50)
    private String fixedInVersion;

    @Column(length = 100)
    private String component;

    @Column(length = 30)
    private String reproducibility;

    @Column(columnDefinition = "TEXT")
    private String impact;

    @Column(columnDefinition = "TEXT")
    private String workaround;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "duplicate_of")
    private Long duplicateOf;

    @Column(name = "reopen_count")
    private Integer reopenCount = 0;

    @Column(length = 50)
    private String source;

    @JsonIgnore
    @OneToMany(mappedBy = "defect", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DefectAttachment> attachments;

    @Column(columnDefinition = "TEXT")
    private String resolution;
}

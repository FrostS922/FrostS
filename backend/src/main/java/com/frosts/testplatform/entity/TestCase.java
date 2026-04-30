package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private TestCaseModule module;

    @Column(columnDefinition = "TEXT")
    private String preconditions;

    @Column(columnDefinition = "TEXT")
    private String steps;

    @Column(columnDefinition = "TEXT")
    private String expectedResults;

    @Column(columnDefinition = "TEXT")
    private String actualResults;

    @Column(length = 20)
    private String type;

    @Column(length = 20)
    private String priority;

    @Column(length = 20)
    private String status;

    @Column(name = "created_by_name", length = 50)
    private String createdByName;

    @Column(length = 50)
    private String automated;
}

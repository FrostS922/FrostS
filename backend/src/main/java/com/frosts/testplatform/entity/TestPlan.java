package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "test_plan")
public class TestPlan extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 20)
    private String status;

    @Column(length = 50)
    private String owner;

    @OneToMany(mappedBy = "testPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestPlanCase> testPlanCases;

    @Column(columnDefinition = "TEXT")
    private String testStrategy;

    @Column(columnDefinition = "TEXT")
    private String scope;
}

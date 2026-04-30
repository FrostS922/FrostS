package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "requirement")
public class Requirement extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50, unique = true)
    private String requirementNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Requirement parent;

    @Column(length = 30)
    private String type;

    @Column(length = 20)
    private String priority;

    @Column(length = 20)
    private String status;

    @Column(name = "assigned_to", length = 50)
    private String assignedTo;
}

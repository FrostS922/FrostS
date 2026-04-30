package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "project")
public class Project extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50, unique = true)
    private String code;

    private String description;

    @Column(length = 50)
    private String manager;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 20)
    private String status;

    @ManyToMany
    @JoinTable(
        name = "project_member",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();
}

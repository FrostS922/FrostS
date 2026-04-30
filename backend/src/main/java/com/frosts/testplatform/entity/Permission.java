package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_permission")
public class Permission extends BaseEntity {

    @Column(unique = true, nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    private String description;

    @Column(length = 100)
    private String resource;

    @Column(length = 20)
    private String action;
}

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

    @Column(length = 50)
    private String category;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(length = 50)
    private String icon;

    @Column(name = "menu_url", length = 200)
    private String menuUrl;

    @Column(name = "perm_type", length = 20)
    private String permType;
}

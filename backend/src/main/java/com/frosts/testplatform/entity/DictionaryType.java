package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_dictionary_type")
public class DictionaryType extends BaseEntity {

    @Column(name = "parent_id")
    private Long parentId;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "is_system")
    private Boolean isSystem = false;

    @Transient
    private List<DictionaryType> children = new ArrayList<>();
}

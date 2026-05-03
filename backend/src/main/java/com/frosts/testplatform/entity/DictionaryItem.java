package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_dictionary_item")
public class DictionaryItem extends BaseEntity {

    @Column(name = "type_id", nullable = false)
    private Long typeId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 100)
    private String value;

    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(length = 20)
    private String color;
}

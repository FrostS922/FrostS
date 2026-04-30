package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_system_setting")
public class SystemSetting extends BaseEntity {

    @Column(name = "setting_key", unique = true, nullable = false, length = 100)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 30)
    private String valueType;

    @Column(name = "options_value", length = 500)
    private String options;

    private String description;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean editable = true;
}

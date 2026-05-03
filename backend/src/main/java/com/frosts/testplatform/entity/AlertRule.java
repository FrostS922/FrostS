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
@Table(name = "sys_alert_rule")
public class AlertRule extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String ruleType;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "metric_name", length = 50)
    private String metricName;

    @Column(name = "condition_type", length = 20)
    private String conditionType;

    private Double threshold;

    @Column(length = 10)
    private String comparator;

    @Column(name = "window_minutes")
    private Integer windowMinutes;

    @Column(name = "min_sample_count")
    private Integer minSampleCount;

    @Column(name = "notify_type", length = 20)
    private String notifyType;

    @Column(length = 20)
    private String priority;

    @Column(name = "cooldown_minutes")
    private Integer cooldownMinutes;

    @Column(columnDefinition = "TEXT")
    private String description;
}

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
@Table(name = "sys_performance_log")
public class PerformanceLog extends BaseEntity {

    @Column(name = "metric_name", nullable = false, length = 20)
    private String metricName;

    @Column(name = "metric_value", nullable = false)
    private Double metricValue;

    @Column(name = "rating", length = 20)
    private String rating;

    @Column(name = "page_url", length = 500)
    private String pageUrl;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "extra_info", columnDefinition = "TEXT")
    private String extraInfo;
}

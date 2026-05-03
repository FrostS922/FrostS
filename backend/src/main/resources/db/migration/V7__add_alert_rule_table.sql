CREATE TABLE sys_alert_rule (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    metric_name VARCHAR(50),
    condition_type VARCHAR(20),
    threshold DOUBLE PRECISION,
    comparator VARCHAR(10),
    window_minutes INTEGER,
    min_sample_count INTEGER,
    notify_type VARCHAR(20),
    priority VARCHAR(20),
    cooldown_minutes INTEGER,
    description TEXT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

INSERT INTO sys_alert_rule (name, rule_type, enabled, metric_name, condition_type, threshold, comparator, window_minutes, min_sample_count, notify_type, priority, cooldown_minutes, description) VALUES
('LCP劣化告警', 'PERFORMANCE', TRUE, 'LCP', 'RATIO', 50.0, 'GTE', 30, 5, 'NOTIFICATION', 'HIGH', 60, 'LCP指标poor占比超过阈值时触发'),
('CLS劣化告警', 'PERFORMANCE', TRUE, 'CLS', 'RATIO', 50.0, 'GTE', 30, 5, 'NOTIFICATION', 'HIGH', 60, 'CLS指标poor占比超过阈值时触发'),
('FID劣化告警', 'PERFORMANCE', TRUE, 'FID', 'RATIO', 50.0, 'GTE', 30, 5, 'NOTIFICATION', 'MEDIUM', 60, 'FID指标poor占比超过阈值时触发'),
('TTFB劣化告警', 'PERFORMANCE', TRUE, 'TTFB', 'RATIO', 50.0, 'GTE', 30, 5, 'NOTIFICATION', 'HIGH', 60, 'TTFB指标poor占比超过阈值时触发'),
('错误率飙升', 'ERROR', TRUE, 'ERROR_RATE', 'THRESHOLD', 10.0, 'GTE', 30, 10, 'NOTIFICATION', 'CRITICAL', 30, '错误率超过阈值时触发'),
('异常IP告警', 'SECURITY', TRUE, 'ANOMALOUS_IP_COUNT', 'THRESHOLD', 5.0, 'GTE', 60, 1, 'NOTIFICATION', 'HIGH', 60, '异常IP数量超过阈值时触发');

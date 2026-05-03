ALTER TABLE sys_user ADD COLUMN mfa_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE sys_user ADD COLUMN mfa_secret VARCHAR(256);
ALTER TABLE sys_user ADD COLUMN mfa_backup_codes VARCHAR(1000);

ALTER TABLE sys_refresh_token ADD COLUMN client_ip VARCHAR(50);
ALTER TABLE sys_refresh_token ADD COLUMN user_agent VARCHAR(500);
ALTER TABLE sys_refresh_token ADD COLUMN device_info VARCHAR(200);
ALTER TABLE sys_refresh_token ADD COLUMN last_refreshed_at TIMESTAMP;

INSERT INTO sys_system_setting (setting_key, setting_value, default_value, name, category, value_type, description, sort_order, is_deleted, created_at, updated_at)
VALUES
('security.weekly_report.enabled', 'true', 'true', '启用安全周报', 'NOTIFICATION', 'BOOLEAN', '是否每周自动发送安全周报邮件', 0, false, NOW(), NOW()),
('security.weekly_report.send_day', '1', '1', '周报发送日', 'NOTIFICATION', 'NUMBER', '周报发送日(1=周一,7=周日)', 1, false, NOW(), NOW()),
('security.weekly_report.send_hour', '9', '9', '周报发送时间', 'NOTIFICATION', 'NUMBER', '周报发送时间(0-23时)', 2, false, NOW(), NOW());

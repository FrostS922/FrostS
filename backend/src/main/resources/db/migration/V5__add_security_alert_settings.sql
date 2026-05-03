INSERT INTO system_setting (setting_key, setting_value, name, description, value_type, category, editable, is_deleted, created_at, updated_at)
VALUES
    ('security.login.warn_threshold', '5', '登录预警阈值', '同一IP在窗口时间内登录失败达到此次数时发送预警通知', 'NUMBER', 'SECURITY', true, false, NOW(), NOW()),
    ('security.login.alert_threshold', '10', '登录告警阈值', '同一IP在窗口时间内登录失败达到此次数时发送紧急告警', 'NUMBER', 'SECURITY', true, false, NOW(), NOW()),
    ('security.login.ban_threshold', '20', 'IP自动封禁阈值', '同一IP在窗口时间内登录失败达到此次数时自动封禁该IP', 'NUMBER', 'SECURITY', true, false, NOW(), NOW()),
    ('security.login.window_minutes', '5', '检测窗口时间(分钟)', '登录异常检测的滑动窗口时间，单位为分钟', 'NUMBER', 'SECURITY', true, false, NOW(), NOW()),
    ('security.login.ban_minutes', '30', 'IP封禁时长(分钟)', '自动封禁IP的持续时间，单位为分钟', 'NUMBER', 'SECURITY', true, false, NOW(), NOW())
ON CONFLICT (setting_key) WHERE is_deleted = false DO NOTHING;

CREATE TABLE sys_notification (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    type VARCHAR(20) NOT NULL,
    category VARCHAR(50),
    priority VARCHAR(10) DEFAULT 'NORMAL',
    sender_id BIGINT REFERENCES sys_user(id),
    target_type VARCHAR(30),
    target_id BIGINT,
    target_url VARCHAR(500),
    is_global BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_notification_type ON sys_notification(type);
CREATE INDEX idx_notification_category ON sys_notification(category);
CREATE INDEX idx_notification_global ON sys_notification(is_global);
CREATE INDEX idx_notification_created_at ON sys_notification(created_at DESC);

CREATE TABLE sys_notification_recipient (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT NOT NULL REFERENCES sys_notification(id),
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    is_starred BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_recipient_user_id ON sys_notification_recipient(user_id);
CREATE INDEX idx_recipient_notification_id ON sys_notification_recipient(notification_id);
CREATE INDEX idx_recipient_user_read ON sys_notification_recipient(user_id, is_read);
CREATE INDEX idx_recipient_user_deleted ON sys_notification_recipient(user_id, is_deleted);

CREATE TABLE sys_notification_preference (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES sys_user(id),
    type_settings JSONB DEFAULT '{"SYSTEM":true,"BUSINESS":true,"REMINDER":true,"TODO":true}',
    category_settings JSONB DEFAULT '{}',
    receive_channels JSONB DEFAULT '{"in_app":true,"email":false}',
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_preference_user_id ON sys_notification_preference(user_id);

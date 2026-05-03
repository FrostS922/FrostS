-- 字典分类表
CREATE TABLE IF NOT EXISTS sys_dictionary_type (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES sys_dictionary_type(id),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    sort_order INT DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_dict_type_parent ON sys_dictionary_type(parent_id);
CREATE INDEX idx_dict_type_code ON sys_dictionary_type(code);
CREATE INDEX idx_dict_type_enabled ON sys_dictionary_type(enabled);

-- 字典枚举值表
CREATE TABLE IF NOT EXISTS sys_dictionary_item (
    id BIGSERIAL PRIMARY KEY,
    type_id BIGINT NOT NULL REFERENCES sys_dictionary_type(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    value VARCHAR(100),
    description TEXT,
    sort_order INT DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    is_default BOOLEAN DEFAULT FALSE,
    color VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    is_deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(type_id, code)
);

CREATE INDEX idx_dict_item_type ON sys_dictionary_item(type_id);
CREATE INDEX idx_dict_item_code ON sys_dictionary_item(code);
CREATE INDEX idx_dict_item_enabled ON sys_dictionary_item(enabled);

-- 字典分类权限表
CREATE TABLE IF NOT EXISTS sys_dictionary_type_role (
    id BIGSERIAL PRIMARY KEY,
    type_id BIGINT NOT NULL REFERENCES sys_dictionary_type(id),
    role_id BIGINT NOT NULL REFERENCES sys_role(id),
    permission VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(type_id, role_id)
);

CREATE INDEX idx_dict_perm_type ON sys_dictionary_type_role(type_id);
CREATE INDEX idx_dict_perm_role ON sys_dictionary_type_role(role_id);

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_dictionary_log (
    id BIGSERIAL PRIMARY KEY,
    type_id BIGINT,
    item_id BIGINT,
    action VARCHAR(20) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    operator VARCHAR(50) NOT NULL,
    operated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50)
);

CREATE INDEX idx_dict_log_type ON sys_dictionary_log(type_id);
CREATE INDEX idx_dict_log_item ON sys_dictionary_log(item_id);
CREATE INDEX idx_dict_log_time ON sys_dictionary_log(operated_at);

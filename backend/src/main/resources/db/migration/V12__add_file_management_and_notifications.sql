-- V12: 文件管理表 + API 接口请求/响应字段扩展
CREATE TABLE IF NOT EXISTS file_attachments (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    agent_id BIGINT,
    original_name VARCHAR(500) NOT NULL,
    stored_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    content_type VARCHAR(200),
    file_category VARCHAR(50),
    uploaded_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_file_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_file_attachments_tenant ON file_attachments(tenant_id);
CREATE INDEX idx_file_attachments_agent ON file_attachments(agent_id);
CREATE INDEX idx_file_attachments_category ON file_attachments(file_category);

-- API 接口表增加请求/响应字段
ALTER TABLE api_interfaces ADD COLUMN IF NOT EXISTS request_headers TEXT;
ALTER TABLE api_interfaces ADD COLUMN IF NOT EXISTS request_body TEXT;
ALTER TABLE api_interfaces ADD COLUMN IF NOT EXISTS response_headers TEXT;
ALTER TABLE api_interfaces ADD COLUMN IF NOT EXISTS response_body TEXT;
ALTER TABLE api_interfaces ADD COLUMN IF NOT EXISTS auth_type VARCHAR(20) DEFAULT 'none';
ALTER TABLE api_interfaces ADD COLUMN IF NOT EXISTS auth_config TEXT;

-- 通知记录表
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    type VARCHAR(50) NOT NULL DEFAULT 'info',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    source VARCHAR(100),
    source_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_tenant ON notifications(tenant_id);
CREATE INDEX idx_notifications_type ON notifications(type);

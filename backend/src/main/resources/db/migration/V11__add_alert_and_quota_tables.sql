-- V11: 添加告警管理和租户配额字段

-- 告警规则表
CREATE TABLE IF NOT EXISTS alert_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    alert_type VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    threshold DECIMAL(15,4),
    comparison_operator VARCHAR(10) DEFAULT 'gt',
    duration_seconds INTEGER DEFAULT 300,
    severity VARCHAR(20) DEFAULT 'WARNING',
    is_active BOOLEAN DEFAULT true,
    notify_channels VARCHAR(200) DEFAULT 'email',
    notify_targets TEXT,
    tenant_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 告警记录表
CREATE TABLE IF NOT EXISTS alert_records (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT,
    rule_name VARCHAR(200),
    alert_type VARCHAR(50),
    severity VARCHAR(20),
    message TEXT,
    metric_value DECIMAL(15,4),
    threshold DECIMAL(15,4),
    status VARCHAR(20) DEFAULT 'firing',
    fired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    tenant_id BIGINT
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_alert_rules_active ON alert_rules(is_active);
CREATE INDEX IF NOT EXISTS idx_alert_records_status ON alert_records(status, fired_at);
CREATE INDEX IF NOT EXISTS idx_alert_records_tenant ON alert_records(tenant_id);

-- 租户表添加配额字段
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS max_agents INTEGER DEFAULT 100;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS max_api_calls_per_day BIGINT DEFAULT 10000;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS max_tokens_per_day BIGINT DEFAULT 1000000;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS max_mcp_calls_per_day BIGINT DEFAULT 5000;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS max_storage_mb BIGINT DEFAULT 1024;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS used_agents INTEGER DEFAULT 0;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS used_api_calls_today BIGINT DEFAULT 0;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS used_tokens_today BIGINT DEFAULT 0;

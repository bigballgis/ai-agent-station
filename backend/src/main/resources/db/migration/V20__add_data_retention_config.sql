-- V20: Add data_retention_policies table for GDPR/privacy compliance
-- Stores configurable retention policies for different data types

CREATE TABLE IF NOT EXISTS data_retention_policies (
    id BIGSERIAL PRIMARY KEY,
    data_type VARCHAR(100) NOT NULL UNIQUE,
    retention_days INTEGER NOT NULL DEFAULT 90,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    cleanup_cron VARCHAR(50) DEFAULT '0 0 3 * * ?',
    last_cleanup_at TIMESTAMP,
    last_cleanup_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

-- Insert default retention policies
INSERT INTO data_retention_policies (data_type, retention_days, description) VALUES
    ('system_logs', 90, '系统日志保留90天'),
    ('test_results', 180, '测试结果保留180天'),
    ('audit_logs', 365, '审计日志保留365天'),
    ('login_logs', 180, '登录日志保留180天'),
    ('user_sessions', 30, '用户会话保留30天'),
    ('api_call_logs', 90, 'API调用日志保留90天')
ON CONFLICT (data_type) DO NOTHING;

-- Create index for active policies
CREATE INDEX idx_data_retention_policies_active ON data_retention_policies(is_active);

-- Add comment
COMMENT ON TABLE data_retention_policies IS '数据保留策略表，用于GDPR/隐私合规';
COMMENT ON COLUMN data_retention_policies.data_type IS '数据类型（system_logs, test_results等）';
COMMENT ON COLUMN data_retention_policies.retention_days IS '保留天数';
COMMENT ON COLUMN data_retention_policies.cleanup_cron IS '清理定时任务Cron表达式';
COMMENT ON COLUMN data_retention_policies.last_cleanup_at IS '上次清理执行时间';
COMMENT ON COLUMN data_retention_policies.last_cleanup_count IS '上次清理删除的记录数';

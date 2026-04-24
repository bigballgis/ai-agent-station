-- =====================================================
-- V30: Add api_call_audit_logs table for Round 268
-- =====================================================

-- API 调用审计日志表（用于持久化请求/响应日志）
CREATE TABLE IF NOT EXISTS api_call_audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT,
    user_id         BIGINT,
    request_id      VARCHAR(100) NOT NULL,
    trace_id        VARCHAR(100),
    client_ip       VARCHAR(45),
    user_agent      VARCHAR(500),
    accept_language VARCHAR(100),
    request_method  VARCHAR(10) NOT NULL,
    request_path    VARCHAR(500) NOT NULL,
    query_params    TEXT,
    request_body    TEXT,
    response_status INTEGER,
    response_body   TEXT,
    execution_time  INTEGER,
    log_level       VARCHAR(10) DEFAULT 'INFO',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引: 按租户+时间查询
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_created
    ON api_call_audit_logs(tenant_id, created_at DESC);

-- 索引: 按请求ID查询
CREATE INDEX IF NOT EXISTS idx_audit_logs_request_id
    ON api_call_audit_logs(request_id);

-- 索引: 按租户+状态查询
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_status
    ON api_call_audit_logs(tenant_id, response_status, created_at DESC);

-- 索引: 按租户+路径查询
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_path
    ON api_call_audit_logs(tenant_id, request_path, created_at DESC);

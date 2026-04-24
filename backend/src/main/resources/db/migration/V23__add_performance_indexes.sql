-- =====================================================
-- V23: Add performance indexes for commonly queried fields
-- =====================================================

-- agents table: add status and created_at indexes
-- (tenant_id already indexed in V3)
CREATE INDEX IF NOT EXISTS idx_agents_status ON agents(status);
CREATE INDEX IF NOT EXISTS idx_agents_created_at ON agents(created_at DESC);
-- Composite index for tenant + status queries (common filter pattern)
CREATE INDEX IF NOT EXISTS idx_agents_tenant_status ON agents(tenant_id, status);

-- execution_history table: add status index
-- (agent_id, tenant_id, timestamp already indexed in V22)
CREATE INDEX IF NOT EXISTS idx_execution_history_status ON execution_history(status);
-- Composite index for tenant + agent + timestamp (common query pattern)
CREATE INDEX IF NOT EXISTS idx_execution_history_tenant_agent_ts ON execution_history(tenant_id, agent_id, timestamp DESC);

-- system_logs table: add module single-column index
-- (tenant_id, user_id, created_at already indexed; module+created_at composite exists in V9)
CREATE INDEX IF NOT EXISTS idx_system_logs_module ON system_logs(module);
-- Composite index for tenant + module queries
CREATE INDEX IF NOT EXISTS idx_system_logs_tenant_module ON system_logs(tenant_id, module);

-- alert_records table: add rule_id index
-- (status+fired_at, tenant_id already indexed in V11)
CREATE INDEX IF NOT EXISTS idx_alert_records_rule_id ON alert_records(rule_id);
-- Composite index for tenant + status queries
CREATE INDEX IF NOT EXISTS idx_alert_records_tenant_status ON alert_records(tenant_id, status);

-- api_call_logs table: add created_at index
-- (agent_id, tenant_id already indexed in V5/V9)
CREATE INDEX IF NOT EXISTS idx_api_call_logs_created_at ON api_call_logs(created_at DESC);
-- Composite index for tenant + agent + created_at (common analytics query)
CREATE INDEX IF NOT EXISTS idx_api_call_logs_tenant_agent_created ON api_call_logs(tenant_id, agent_id, created_at DESC);

-- mcp_tool_call_logs table: add missing indexes
CREATE INDEX IF NOT EXISTS idx_mcp_tool_call_logs_tenant ON mcp_tool_call_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mcp_tool_call_logs_tool ON mcp_tool_call_logs(mcp_tool_id);
CREATE INDEX IF NOT EXISTS idx_mcp_tool_call_logs_api_call ON mcp_tool_call_logs(api_call_log_id);
CREATE INDEX IF NOT EXISTS idx_mcp_tool_call_logs_created_at ON mcp_tool_call_logs(created_at DESC);

-- login_logs table: add status index
-- (user_id, login_time, tenant_id already indexed in V15)
CREATE INDEX IF NOT EXISTS idx_login_logs_status ON login_logs(status);
-- Composite index for tenant + user + time (common audit query)
CREATE INDEX IF NOT EXISTS idx_login_logs_tenant_user_time ON login_logs(tenant_id, user_id, login_time DESC);

-- data_change_logs table: add operator index
-- (table_name+record_id, operated_at, tenant_id already indexed in V14)
CREATE INDEX IF NOT EXISTS idx_dcl_operator ON data_change_logs(operator);

-- alert_rules table: add tenant_id index
-- (is_active already indexed in V11)
CREATE INDEX IF NOT EXISTS idx_alert_rules_tenant ON alert_rules(tenant_id);

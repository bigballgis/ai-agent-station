-- API调用状态枚举类型
CREATE TYPE IF NOT EXISTS api_call_status AS ENUM ('SUCCESS', 'FAILED', 'TIMEOUT', 'RATE_LIMITED', 'UNAUTHORIZED');

-- API调用日志表
CREATE TABLE IF NOT EXISTS api_call_logs (
  id BIGSERIAL PRIMARY KEY,
  api_interface_id BIGINT,
  agent_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  user_id BIGINT,
  request_id VARCHAR(100) NOT NULL,
  request_method VARCHAR(10) NOT NULL,
  request_path VARCHAR(500) NOT NULL,
  request_headers TEXT,
  request_params TEXT,
  request_body TEXT,
  response_status INTEGER,
  response_headers TEXT,
  response_body TEXT,
  status api_call_status NOT NULL DEFAULT 'SUCCESS',
  error_message TEXT,
  execution_time INTEGER,
  is_async BOOLEAN NOT NULL DEFAULT false,
  async_task_id VARCHAR(100),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 限流配置表
CREATE TABLE IF NOT EXISTS rate_limit_configs (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  agent_id BIGINT,
  api_interface_id BIGINT,
  limit_type VARCHAR(20) NOT NULL DEFAULT 'GLOBAL',
  requests_per_second INTEGER NOT NULL DEFAULT 10,
  requests_per_minute INTEGER NOT NULL DEFAULT 100,
  requests_per_hour INTEGER NOT NULL DEFAULT 1000,
  requests_per_day INTEGER NOT NULL DEFAULT 10000,
  burst_capacity INTEGER NOT NULL DEFAULT 20,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- MCP工具注册表
CREATE TABLE IF NOT EXISTS mcp_tools (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  tool_name VARCHAR(100) NOT NULL,
  tool_code VARCHAR(100) NOT NULL,
  tool_type VARCHAR(50) NOT NULL,
  description TEXT,
  endpoint_url VARCHAR(500),
  config TEXT,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_mcp_tool_code UNIQUE (tenant_id, tool_code)
);

-- MCP工具调用日志表
CREATE TABLE IF NOT EXISTS mcp_tool_call_logs (
  id BIGSERIAL PRIMARY KEY,
  mcp_tool_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  api_call_log_id BIGINT,
  request_params TEXT,
  response_result TEXT,
  status api_call_status NOT NULL DEFAULT 'SUCCESS',
  error_message TEXT,
  execution_time INTEGER,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_api_call_logs_agent_id ON api_call_logs(agent_id);
CREATE INDEX IF NOT EXISTS idx_api_call_logs_tenant_id ON api_call_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_api_call_logs_request_id ON api_call_logs(request_id);
CREATE INDEX IF NOT EXISTS idx_api_call_logs_created_at ON api_call_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_api_call_logs_status ON api_call_logs(status);

CREATE INDEX IF NOT EXISTS idx_rate_limit_configs_tenant_id ON rate_limit_configs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_rate_limit_configs_agent_id ON rate_limit_configs(agent_id);

CREATE INDEX IF NOT EXISTS idx_mcp_tools_tenant_id ON mcp_tools(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mcp_tools_is_active ON mcp_tools(is_active);

CREATE INDEX IF NOT EXISTS idx_mcp_tool_call_logs_mcp_tool_id ON mcp_tool_call_logs(mcp_tool_id);
CREATE INDEX IF NOT EXISTS idx_mcp_tool_call_logs_tenant_id ON mcp_tool_call_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_mcp_tool_call_logs_api_call_log_id ON mcp_tool_call_logs(api_call_log_id);

-- 外键约束
ALTER TABLE api_call_logs ADD CONSTRAINT fk_api_call_logs_api_interface FOREIGN KEY (api_interface_id) REFERENCES api_interfaces(id) ON DELETE SET NULL;
ALTER TABLE api_call_logs ADD CONSTRAINT fk_api_call_logs_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
ALTER TABLE api_call_logs ADD CONSTRAINT fk_api_call_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

ALTER TABLE rate_limit_configs ADD CONSTRAINT fk_rate_limit_configs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE rate_limit_configs ADD CONSTRAINT fk_rate_limit_configs_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
ALTER TABLE rate_limit_configs ADD CONSTRAINT fk_rate_limit_configs_api_interface FOREIGN KEY (api_interface_id) REFERENCES api_interfaces(id) ON DELETE CASCADE;

ALTER TABLE mcp_tools ADD CONSTRAINT fk_mcp_tools_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

ALTER TABLE mcp_tool_call_logs ADD CONSTRAINT fk_mcp_tool_call_logs_mcp_tool FOREIGN KEY (mcp_tool_id) REFERENCES mcp_tools(id) ON DELETE CASCADE;
ALTER TABLE mcp_tool_call_logs ADD CONSTRAINT fk_mcp_tool_call_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE mcp_tool_call_logs ADD CONSTRAINT fk_mcp_tool_call_logs_api_call_log FOREIGN KEY (api_call_log_id) REFERENCES api_call_logs(id) ON DELETE SET NULL;

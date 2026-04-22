-- 创建时间戳更新函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 系统配置表 (公共schema)
CREATE TABLE system_config (
  id BIGSERIAL PRIMARY KEY,
  config_key VARCHAR(100) NOT NULL,
  config_value TEXT,
  config_type VARCHAR(20) NOT NULL DEFAULT 'string',
  description VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_config_key UNIQUE (config_key)
);

CREATE TRIGGER update_system_config_updated_at BEFORE UPDATE ON system_config
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 租户表 (公共schema)
CREATE TABLE tenant (
  id BIGSERIAL PRIMARY KEY,
  tenant_name VARCHAR(100) NOT NULL,
  tenant_code VARCHAR(50) NOT NULL,
  tenant_schema VARCHAR(50) NOT NULL,
  status SMALLINT NOT NULL DEFAULT 1,
  max_users INTEGER DEFAULT 100,
  max_agents INTEGER DEFAULT 50,
  expire_date TIMESTAMP,
  config JSONB DEFAULT '{}'::jsonb,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_tenant_code UNIQUE (tenant_code),
  CONSTRAINT uk_tenant_schema UNIQUE (tenant_schema)
);

CREATE TRIGGER update_tenant_updated_at BEFORE UPDATE ON tenant
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 创建租户schema的函数
CREATE OR REPLACE FUNCTION create_tenant_schema(p_tenant_id BIGINT, p_tenant_schema VARCHAR)
RETURNS VOID AS $$
DECLARE
  v_schema_name VARCHAR;
BEGIN
  v_schema_name := quote_ident(p_tenant_schema);
  EXECUTE 'CREATE SCHEMA IF NOT EXISTS ' || v_schema_name;
  
  -- 在租户schema中创建用户表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '."user" (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar VARCHAR(255),
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_username_' || p_tenant_id || ' UNIQUE (username)
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_user_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '."user"(tenant_id)';
  
  EXECUTE 'CREATE OR REPLACE TRIGGER update_user_updated_at_' || p_tenant_id || ' BEFORE UPDATE ON ' || v_schema_name || '."user"
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()';
    
  -- 在租户schema中创建角色表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.role (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_role_code_' || p_tenant_id || ' UNIQUE (role_code, tenant_id)
  )';
  
  EXECUTE 'CREATE OR REPLACE TRIGGER update_role_updated_at_' || p_tenant_id || ' BEFORE UPDATE ON ' || v_schema_name || '.role
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()';
    
  -- 在租户schema中创建用户角色关联表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_role_' || p_tenant_id || ' UNIQUE (user_id, role_id)
  )';
  
  -- 在租户schema中创建权限表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.permission (
    id BIGSERIAL PRIMARY KEY,
    permission_name VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_permission_code_' || p_tenant_id || ' UNIQUE (permission_code)
  )';
  
  EXECUTE 'CREATE OR REPLACE TRIGGER update_permission_updated_at_' || p_tenant_id || ' BEFORE UPDATE ON ' || v_schema_name || '.permission
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()';
    
  -- 在租户schema中创建角色权限关联表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.role_permission (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_role_permission_' || p_tenant_id || ' UNIQUE (role_id, permission_id)
  )';
  
  -- 在租户schema中创建Agent表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.agent (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    agent_name VARCHAR(100) NOT NULL,
    agent_code VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    icon VARCHAR(255),
    language VARCHAR(10) NOT NULL DEFAULT ''zh-CN'',
    status SMALLINT NOT NULL DEFAULT 0,
    config JSONB NOT NULL DEFAULT ''{}''::jsonb,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_agent_code_' || p_tenant_id || ' UNIQUE (agent_code)
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent(tenant_id)';
  
  EXECUTE 'CREATE OR REPLACE TRIGGER update_agent_updated_at_' || p_tenant_id || ' BEFORE UPDATE ON ' || v_schema_name || '.agent
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()';
    
  -- 在租户schema中创建Agent版本表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.agent_version (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    version VARCHAR(20) NOT NULL,
    config JSONB NOT NULL DEFAULT ''{}''::jsonb,
    status SMALLINT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_version_agent_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_version(agent_id)';
  
  -- 在租户schema中创建Agent审批表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.agent_approval (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    status SMALLINT NOT NULL DEFAULT 0,
    submit_by BIGINT NOT NULL,
    approve_by BIGINT,
    approve_opinion VARCHAR(500),
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_approval_agent_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_approval(agent_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_approval_version_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_approval(version_id)';
  
  -- 在租户schema中创建API接口表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.api_interface (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    api_path VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL DEFAULT ''POST'',
    status SMALLINT NOT NULL DEFAULT 0,
    config JSONB NOT NULL DEFAULT ''{}''::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_api_path_' || p_tenant_id || ' UNIQUE (api_path)
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_api_interface_agent_id_' || p_tenant_id || ' ON ' || v_schema_name || '.api_interface(agent_id)';
  
  EXECUTE 'CREATE OR REPLACE TRIGGER update_api_interface_updated_at_' || p_tenant_id || ' BEFORE UPDATE ON ' || v_schema_name || '.api_interface
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()';
    
  -- 在租户schema中创建MCP服务器表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.mcp_server (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    server_name VARCHAR(100) NOT NULL,
    server_code VARCHAR(50) NOT NULL,
    server_type VARCHAR(20) NOT NULL,
    config JSONB NOT NULL DEFAULT ''{}''::jsonb,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mcp_server_code_' || p_tenant_id || ' UNIQUE (server_code)
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_mcp_server_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '.mcp_server(tenant_id)';
  
  EXECUTE 'CREATE OR REPLACE TRIGGER update_mcp_server_updated_at_' || p_tenant_id || ' BEFORE UPDATE ON ' || v_schema_name || '.mcp_server
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()';
    
  -- 在租户schema中创建MCP工具表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.mcp_tool (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    tool_name VARCHAR(100) NOT NULL,
    tool_code VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    input_schema JSONB NOT NULL DEFAULT ''{}''::jsonb,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_mcp_tool_code_' || p_tenant_id || ' UNIQUE (tool_code)
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_mcp_tool_server_id_' || p_tenant_id || ' ON ' || v_schema_name || '.mcp_tool(server_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_mcp_tool_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '.mcp_tool(tenant_id)';
  
  EXECUTE 'CREATE OR REPLACE TRIGGER update_mcp_tool_updated_at_' || p_tenant_id || ' BEFORE UPDATE ON ' || v_schema_name || '.mcp_tool
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()';
    
  -- 在租户schema中创建模型配置表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.model_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    model_code VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    api_key VARCHAR(255) NOT NULL,
    api_url VARCHAR(255) NOT NULL,
    config JSONB NOT NULL DEFAULT ''{}''::jsonb,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_model_config_code_' || p_tenant_id || ' UNIQUE (model_code)
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_model_config_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '.model_config(tenant_id)';
  
  EXECUTE 'CREATE OR REPLACE TRIGGER update_model_config_updated_at_' || p_tenant_id || ' BEFORE UPDATE ON ' || v_schema_name || '.model_config
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()';
    
  -- 在租户schema中创建记忆表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.memory (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    session_id VARCHAR(100),
    user_id BIGINT,
    memory_type VARCHAR(20) NOT NULL,
    content JSONB NOT NULL DEFAULT ''{}''::jsonb,
    embedding_vector vector(1536),
    metadata JSONB DEFAULT ''{}''::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_memory_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '.memory(tenant_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_memory_agent_id_' || p_tenant_id || ' ON ' || v_schema_name || '.memory(agent_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_memory_session_id_' || p_tenant_id || ' ON ' || v_schema_name || '.memory(session_id)';
  
  EXECUTE 'CREATE OR REPLACE TRIGGER update_memory_updated_at_' || p_tenant_id || ' BEFORE UPDATE ON ' || v_schema_name || '.memory
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()';
    
  -- 在租户schema中创建系统操作日志表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.system_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    tenant_id BIGINT NOT NULL,
    operation VARCHAR(255) NOT NULL,
    module VARCHAR(100) NOT NULL,
    ip VARCHAR(50),
    request_params JSONB,
    response_result JSONB,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_system_log_user_id_' || p_tenant_id || ' ON ' || v_schema_name || '.system_log(user_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_system_log_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '.system_log(tenant_id)';
  
  -- 在租户schema中创建API调用日志表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.api_call_log (
    id BIGSERIAL PRIMARY KEY,
    api_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    request_params JSONB,
    response_result JSONB,
    status SMALLINT NOT NULL DEFAULT 0,
    response_time INTEGER,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_api_call_log_api_id_' || p_tenant_id || ' ON ' || v_schema_name || '.api_call_log(api_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_api_call_log_agent_id_' || p_tenant_id || ' ON ' || v_schema_name || '.api_call_log(agent_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_api_call_log_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '.api_call_log(tenant_id)';
  
END;
$$ LANGUAGE plpgsql;

-- 初始化系统配置数据
INSERT INTO system_config (config_key, config_value, config_type, description) VALUES
('system.name', 'AI Agent Platform', 'string', '系统名称'),
('system.version', '1.0.0', 'string', '系统版本'),
('system.default.language', 'zh-CN', 'string', '默认语言'),
('tenant.default.schema.prefix', 't_', 'string', '租户schema前缀'),
('agent.default.timeout', '30', 'number', 'Agent默认超时时间(秒)'),
('api.rate.limit.enabled', 'true', 'boolean', 'API限流是否启用'),
('api.rate.limit.requests', '100', 'number', 'API限流请求数'),
('api.rate.limit.period', '60', 'number', 'API限流周期(秒)');

-- 初始化公共权限数据
INSERT INTO permission (permission_name, permission_code, description) VALUES
('Agent设计', 'AGENT_DESIGN', '创建和编辑Agent'),
('Agent审批', 'AGENT_APPROVAL', '审批Agent配置'),
('Agent发布', 'AGENT_PUBLISH', '发布和回滚Agent'),
('Agent调试', 'AGENT_DEBUG', '在线调试Agent'),
('API管理', 'API_MANAGEMENT', '管理API接口'),
('租户管理', 'TENANT_MANAGEMENT', '管理租户信息'),
('用户管理', 'USER_MANAGEMENT', '管理用户账户'),
('角色管理', 'ROLE_MANAGEMENT', '管理角色权限'),
('MCP管理', 'MCP_MANAGEMENT', '管理MCP服务器和工具'),
('模型配置', 'MODEL_CONFIG', '配置大模型'),
('日志查看', 'LOG_VIEW', '查看系统日志'),
('统计分析', 'ANALYTICS', '查看统计数据');

-- 初始化公共角色数据
INSERT INTO role (role_name, role_code, description) VALUES
('超级管理员', 'SUPER_ADMIN', '拥有系统所有权限'),
('租户管理员', 'TENANT_ADMIN', '管理租户下的所有资源'),
('Agent设计者', 'AGENT_DESIGNER', '设计和开发AI Agent'),
('审批者', 'APPROVER', '审批Agent配置'),
('观察者', 'VIEWER', '只能查看资源');

-- 为角色分配权限
WITH super_admin_role AS (SELECT id FROM role WHERE role_code = 'SUPER_ADMIN'),
tenant_admin_role AS (SELECT id FROM role WHERE role_code = 'TENANT_ADMIN'),
agent_designer_role AS (SELECT id FROM role WHERE role_code = 'AGENT_DESIGNER'),
approver_role AS (SELECT id FROM role WHERE role_code = 'APPROVER'),
viewer_role AS (SELECT id FROM role WHERE role_code = 'VIEWER')

-- 超级管理员拥有所有权限
INSERT INTO role_permission (role_id, permission_id)
SELECT (SELECT id FROM super_admin_role), id FROM permission;

-- 租户管理员拥有除租户管理外的所有权限
INSERT INTO role_permission (role_id, permission_id)
SELECT (SELECT id FROM tenant_admin_role), id FROM permission WHERE permission_code != 'TENANT_MANAGEMENT';

-- Agent设计者拥有Agent相关权限
INSERT INTO role_permission (role_id, permission_id)
SELECT (SELECT id FROM agent_designer_role), id FROM permission 
WHERE permission_code IN ('AGENT_DESIGN', 'AGENT_DEBUG', 'API_MANAGEMENT', 'MCP_MANAGEMENT', 'LOG_VIEW');

-- 审批者拥有审批和查看权限
INSERT INTO role_permission (role_id, permission_id)
SELECT (SELECT id FROM approver_role), id FROM permission 
WHERE permission_code IN ('AGENT_APPROVAL', 'LOG_VIEW');

-- 观察者只有查看权限
INSERT INTO role_permission (role_id, permission_id)
SELECT (SELECT id FROM viewer_role), id FROM permission 
WHERE permission_code IN ('LOG_VIEW', 'ANALYTICS');

-- 创建默认租户
INSERT INTO tenant (tenant_name, tenant_code, tenant_schema, status, max_users, max_agents, config) VALUES
('默认租户', 'default', 't_default', 1, 1000, 500, '{"enable_analytics": true, "enable_mcp": true}'::jsonb);

-- 创建默认租户schema
SELECT create_tenant_schema(1, 't_default');

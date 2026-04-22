-- 反思评估表
CREATE TABLE IF NOT EXISTS agent_evolution_reflections (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  agent_id BIGINT NOT NULL,
  evaluation_type VARCHAR(50) NOT NULL,
  evaluation_metrics JSONB NOT NULL DEFAULT '{}'::jsonb,
  performance_score DECIMAL(5,2),
  accuracy_score DECIMAL(5,2),
  efficiency_score DECIMAL(5,2),
  user_satisfaction_score DECIMAL(5,2),
  strengths TEXT[],
  weaknesses TEXT[],
  summary TEXT,
  status SMALLINT NOT NULL DEFAULT 1,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_evolution_reflections_tenant_id ON agent_evolution_reflections(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_evolution_reflections_agent_id ON agent_evolution_reflections(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_evolution_reflections_evaluation_type ON agent_evolution_reflections(evaluation_type);

CREATE TRIGGER update_agent_evolution_reflections_updated_at BEFORE UPDATE ON agent_evolution_reflections
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 经验沉淀表
CREATE TABLE IF NOT EXISTS agent_evolution_experiences (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  agent_id BIGINT NOT NULL,
  experience_type VARCHAR(50) NOT NULL,
  experience_code VARCHAR(50) NOT NULL,
  title VARCHAR(100) NOT NULL,
  description TEXT,
  content JSONB NOT NULL DEFAULT '{}'::jsonb,
  tags TEXT[],
  usage_count INTEGER NOT NULL DEFAULT 0,
  effectiveness_score DECIMAL(5,2),
  status SMALLINT NOT NULL DEFAULT 1,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_experience_code UNIQUE (experience_code, tenant_id)
);

CREATE INDEX IF NOT EXISTS idx_agent_evolution_experiences_tenant_id ON agent_evolution_experiences(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_evolution_experiences_agent_id ON agent_evolution_experiences(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_evolution_experiences_experience_type ON agent_evolution_experiences(experience_type);
CREATE INDEX IF NOT EXISTS idx_agent_evolution_experiences_tags ON agent_evolution_experiences USING GIN (tags);

CREATE TRIGGER update_agent_evolution_experiences_updated_at BEFORE UPDATE ON agent_evolution_experiences
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 优化建议表
CREATE TABLE IF NOT EXISTS agent_evolution_suggestions (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  agent_id BIGINT NOT NULL,
  reflection_id BIGINT,
  suggestion_type VARCHAR(50) NOT NULL,
  title VARCHAR(100) NOT NULL,
  description TEXT,
  content JSONB NOT NULL DEFAULT '{}'::jsonb,
  priority SMALLINT NOT NULL DEFAULT 1,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  implementation_status VARCHAR(20) NOT NULL DEFAULT 'not_started',
  expected_impact DECIMAL(5,2),
  actual_impact DECIMAL(5,2),
  implemented_by BIGINT,
  implemented_at TIMESTAMP,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_evolution_suggestions_tenant_id ON agent_evolution_suggestions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_evolution_suggestions_agent_id ON agent_evolution_suggestions(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_evolution_suggestions_reflection_id ON agent_evolution_suggestions(reflection_id);
CREATE INDEX IF NOT EXISTS idx_agent_evolution_suggestions_status ON agent_evolution_suggestions(status);
CREATE INDEX IF NOT EXISTS idx_agent_evolution_suggestions_implementation_status ON agent_evolution_suggestions(implementation_status);

CREATE TRIGGER update_agent_evolution_suggestions_updated_at BEFORE UPDATE ON agent_evolution_suggestions
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 添加外键约束
ALTER TABLE agent_evolution_reflections ADD CONSTRAINT fk_agent_evolution_reflections_agent FOREIGN KEY (agent_id) REFERENCES agent(id);
ALTER TABLE agent_evolution_experiences ADD CONSTRAINT fk_agent_evolution_experiences_agent FOREIGN KEY (agent_id) REFERENCES agent(id);
ALTER TABLE agent_evolution_suggestions ADD CONSTRAINT fk_agent_evolution_suggestions_agent FOREIGN KEY (agent_id) REFERENCES agent(id);
ALTER TABLE agent_evolution_suggestions ADD CONSTRAINT fk_agent_evolution_suggestions_reflection FOREIGN KEY (reflection_id) REFERENCES agent_evolution_reflections(id);

-- 更新创建租户schema的函数，添加自进化相关表
CREATE OR REPLACE FUNCTION create_tenant_schema(p_tenant_id BIGINT, p_tenant_schema VARCHAR)
RETURNS VOID AS $$
DECLARE
  v_schema_name VARCHAR;
BEGIN
  v_schema_name := quote_ident(p_tenant_schema);
  EXECUTE 'CREATE SCHEMA IF NOT EXISTS ' || v_schema_name;
  
  -- 在租户schema中创建用户表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.("user") (
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
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_user_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '.("user")(tenant_id)';
  
  EXECUTE 'CREATE OR REPLACE TRIGGER update_user_updated_at_' || p_tenant_id || ' BEFORE UPDATE ON ' || v_schema_name || '.("user")
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
  
  -- 在租户schema中创建测试用例表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.agent_test_cases (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    test_name VARCHAR(100) NOT NULL,
    test_code VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    test_type VARCHAR(20) NOT NULL DEFAULT ''unit'',
    input_params JSONB NOT NULL DEFAULT ''{}''::jsonb,
    expected_output JSONB NOT NULL DEFAULT ''{}''::jsonb,
    status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_test_code_' || p_tenant_id || ' UNIQUE (test_code, tenant_id)
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_test_cases_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_test_cases(tenant_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_test_cases_agent_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_test_cases(agent_id)';
  
  EXECUTE 'CREATE OR REPLACE TRIGGER update_agent_test_cases_updated_at_' || p_tenant_id || ' BEFORE UPDATE ON ' || v_schema_name || '.agent_test_cases
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()';
  
  -- 在租户schema中创建测试执行记录表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.agent_test_executions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    test_case_id BIGINT NOT NULL,
    execution_type VARCHAR(20) NOT NULL DEFAULT ''manual'',
    executor_id BIGINT NOT NULL,
    status SMALLINT NOT NULL DEFAULT 0,
    start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    execution_time INTEGER,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_test_executions_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_test_executions(tenant_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_test_executions_agent_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_test_executions(agent_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_test_executions_test_case_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_test_executions(test_case_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_test_executions_status_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_test_executions(status)';
  
  -- 在租户schema中创建测试结果表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.agent_test_results (
    id BIGSERIAL PRIMARY KEY,
    execution_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    test_case_id BIGINT NOT NULL,
    actual_output JSONB NOT NULL DEFAULT ''{}''::jsonb,
    expected_output JSONB NOT NULL DEFAULT ''{}''::jsonb,
    status VARCHAR(20) NOT NULL DEFAULT ''pending'',
    comparison_result JSONB DEFAULT ''{}''::jsonb,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_test_results_execution_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_test_results(execution_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_test_results_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_test_results(tenant_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_test_results_agent_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_test_results(agent_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_test_results_test_case_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_test_results(test_case_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_test_results_status_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_test_results(status)';
  
  -- 在租户schema中创建反思评估表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.agent_evolution_reflections (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    evaluation_type VARCHAR(50) NOT NULL,
    evaluation_metrics JSONB NOT NULL DEFAULT ''{}''::jsonb,
    performance_score DECIMAL(5,2),
    accuracy_score DECIMAL(5,2),
    efficiency_score DECIMAL(5,2),
    user_satisfaction_score DECIMAL(5,2),
    strengths TEXT[],
    weaknesses TEXT[],
    summary TEXT,
    status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_evolution_reflections_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_evolution_reflections(tenant_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_evolution_reflections_agent_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_evolution_reflections(agent_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_evolution_reflections_evaluation_type_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_evolution_reflections(evaluation_type)';
  
  EXECUTE 'CREATE OR REPLACE TRIGGER update_agent_evolution_reflections_updated_at_' || p_tenant_id || ' BEFORE UPDATE ON ' || v_schema_name || '.agent_evolution_reflections
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()';
  
  -- 在租户schema中创建经验沉淀表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.agent_evolution_experiences (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    experience_type VARCHAR(50) NOT NULL,
    experience_code VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    content JSONB NOT NULL DEFAULT ''{}''::jsonb,
    tags TEXT[],
    usage_count INTEGER NOT NULL DEFAULT 0,
    effectiveness_score DECIMAL(5,2),
    status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_experience_code_' || p_tenant_id || ' UNIQUE (experience_code, tenant_id)
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_evolution_experiences_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_evolution_experiences(tenant_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_evolution_experiences_agent_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_evolution_experiences(agent_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_evolution_experiences_experience_type_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_evolution_experiences(experience_type)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_evolution_experiences_tags_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_evolution_experiences USING GIN (tags)';
  
  EXECUTE 'CREATE OR REPLACE TRIGGER update_agent_evolution_experiences_updated_at_' || p_tenant_id || ' BEFORE UPDATE ON ' || v_schema_name || '.agent_evolution_experiences
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()';
  
  -- 在租户schema中创建优化建议表
  EXECUTE 'CREATE TABLE IF NOT EXISTS ' || v_schema_name || '.agent_evolution_suggestions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    reflection_id BIGINT,
    suggestion_type VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    content JSONB NOT NULL DEFAULT ''{}''::jsonb,
    priority SMALLINT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT ''pending'',
    implementation_status VARCHAR(20) NOT NULL DEFAULT ''not_started'',
    expected_impact DECIMAL(5,2),
    actual_impact DECIMAL(5,2),
    implemented_by BIGINT,
    implemented_at TIMESTAMP,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  )';
  
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_evolution_suggestions_tenant_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_evolution_suggestions(tenant_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_evolution_suggestions_agent_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_evolution_suggestions(agent_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_evolution_suggestions_reflection_id_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_evolution_suggestions(reflection_id)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_evolution_suggestions_status_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_evolution_suggestions(status)';
  EXECUTE 'CREATE INDEX IF NOT EXISTS idx_agent_evolution_suggestions_implementation_status_' || p_tenant_id || ' ON ' || v_schema_name || '.agent_evolution_suggestions(implementation_status)';
  
  EXECUTE 'CREATE OR REPLACE TRIGGER update_agent_evolution_suggestions_updated_at_' || p_tenant_id || ' BEFORE UPDATE ON ' || v_schema_name || '.agent_evolution_suggestions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()';
  
END;
$$ LANGUAGE plpgsql;

-- 添加自进化相关的系统配置
INSERT INTO system_config (config_key, config_value, config_type, description) VALUES
('evolution.enabled', 'true', 'boolean', '自进化功能是否启用'),
('evolution.evaluation.frequency', '7', 'number', '评估频率(天)'),
('evolution.experience.retention.days', '90', 'number', '经验沉淀保留天数'),
('evolution.suggestion.auto.generate', 'true', 'boolean', '是否自动生成优化建议'),
('evolution.suggestion.priority.threshold', '7', 'number', '建议优先级阈值');

-- 添加自进化相关的权限
INSERT INTO permission (permission_name, permission_code, description) VALUES
('自进化管理', 'EVOLUTION_MANAGEMENT', '管理自进化功能'),
('反思评估', 'REFLECTION_EVALUATION', '进行Agent反思评估'),
('经验沉淀', 'EXPERIENCE_ACCUMULATION', '管理经验沉淀'),
('优化建议', 'OPTIMIZATION_SUGGESTION', '管理优化建议');

-- 为角色分配自进化相关权限
WITH super_admin_role AS (SELECT id FROM role WHERE role_code = 'SUPER_ADMIN'),
tenant_admin_role AS (SELECT id FROM role WHERE role_code = 'TENANT_ADMIN'),
agent_designer_role AS (SELECT id FROM role WHERE role_code = 'AGENT_DESIGNER'),
approver_role AS (SELECT id FROM role WHERE role_code = 'APPROVER'),
viewer_role AS (SELECT id FROM role WHERE role_code = 'VIEWER')

-- 超级管理员拥有所有权限
INSERT INTO role_permission (role_id, permission_id)
SELECT (SELECT id FROM super_admin_role), id FROM permission WHERE permission_code IN ('EVOLUTION_MANAGEMENT', 'REFLECTION_EVALUATION', 'EXPERIENCE_ACCUMULATION', 'OPTIMIZATION_SUGGESTION')
ON CONFLICT DO NOTHING;

-- 租户管理员拥有自进化管理权限
INSERT INTO role_permission (role_id, permission_id)
SELECT (SELECT id FROM tenant_admin_role), id FROM permission WHERE permission_code IN ('EVOLUTION_MANAGEMENT', 'REFLECTION_EVALUATION', 'EXPERIENCE_ACCUMULATION', 'OPTIMIZATION_SUGGESTION')
ON CONFLICT DO NOTHING;

-- Agent设计者拥有反思评估和优化建议权限
INSERT INTO role_permission (role_id, permission_id)
SELECT (SELECT id FROM agent_designer_role), id FROM permission WHERE permission_code IN ('REFLECTION_EVALUATION', 'EXPERIENCE_ACCUMULATION', 'OPTIMIZATION_SUGGESTION')
ON CONFLICT DO NOTHING;

-- 审批者拥有优化建议查看权限
INSERT INTO role_permission (role_id, permission_id)
SELECT (SELECT id FROM approver_role), id FROM permission WHERE permission_code = 'OPTIMIZATION_SUGGESTION'
ON CONFLICT DO NOTHING;

-- 观察者拥有查看权限
INSERT INTO role_permission (role_id, permission_id)
SELECT (SELECT id FROM viewer_role), id FROM permission WHERE permission_code IN ('EXPERIENCE_ACCUMULATION', 'OPTIMIZATION_SUGGESTION')
ON CONFLICT DO NOTHING;
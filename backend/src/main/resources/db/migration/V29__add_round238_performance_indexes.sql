-- =====================================================
-- V29: Add additional performance indexes for Round 238
-- =====================================================

-- api_call_logs: composite index for findByRequestId (unique lookup, very frequent)
CREATE INDEX IF NOT EXISTS idx_api_call_logs_request_id ON api_call_logs(request_id);

-- api_call_logs: composite index for findByTenantIdAndApiInterfaceId pagination queries
CREATE INDEX IF NOT EXISTS idx_api_call_logs_tenant_interface ON api_call_logs(tenant_id, api_interface_id, created_at DESC);

-- api_call_logs: composite index for findByTenantIdAndAgentId pagination queries
CREATE INDEX IF NOT EXISTS idx_api_call_logs_tenant_agent ON api_call_logs(tenant_id, agent_id, created_at DESC);

-- api_interfaces: composite index for findByAgentIdAndTenantId queries
CREATE INDEX IF NOT EXISTS idx_api_interfaces_agent_tenant ON api_interfaces(agent_id, tenant_id);

-- api_interfaces: composite index for findByBaseApiIdAndTenantId queries
CREATE INDEX IF NOT EXISTS idx_api_interfaces_base_tenant ON api_interfaces(base_api_id, tenant_id);

-- mcp_tools: composite index for findByTenantIdAndToolCode (unique lookup)
CREATE UNIQUE INDEX IF NOT EXISTS idx_mcp_tools_tenant_code ON mcp_tools(tenant_id, tool_code);

-- mcp_tools: composite index for findByTenantIdAndIsActiveTrue
CREATE INDEX IF NOT EXISTS idx_mcp_tools_tenant_active ON mcp_tools(tenant_id, is_active);

-- agent_approvals: composite index for findByTenantIdAndStatus pagination
CREATE INDEX IF NOT EXISTS idx_agent_approvals_tenant_status ON agent_approvals(tenant_id, status, submitted_at DESC);

-- agent_versions: composite index for findByAgentIdAndTenantId queries
CREATE INDEX IF NOT EXISTS idx_agent_versions_agent_tenant ON agent_versions(agent_id, tenant_id, version_number DESC);

-- workflow_definitions: composite index for findByTenantIdAndStatus
CREATE INDEX IF NOT EXISTS idx_workflow_defs_tenant_status ON workflow_definitions(tenant_id, status);

-- workflow_definitions: composite index for existsByNameAndTenantId
CREATE UNIQUE INDEX IF NOT EXISTS idx_workflow_defs_name_tenant ON workflow_definitions(name, tenant_id);

-- workflow_instances: composite index for findByWorkflowDefinitionIdAndTenantId
CREATE INDEX IF NOT EXISTS idx_workflow_instances_def_tenant ON workflow_instances(workflow_definition_id, tenant_id, created_at DESC);

-- workflow_instances: composite index for findByTenantIdAndStatus
CREATE INDEX IF NOT EXISTS idx_workflow_instances_tenant_status ON workflow_instances(tenant_id, status, created_at DESC);

-- rate_limit_configs: composite index for findByTenantIdAndLimitTypeAndIsActiveTrue
CREATE INDEX IF NOT EXISTS idx_rate_limit_tenant_type_active ON rate_limit_configs(tenant_id, limit_type, is_active);

-- rate_limit_configs: composite index for findByTenantIdAndAgentIdAndIsActiveTrue
CREATE INDEX IF NOT EXISTS idx_rate_limit_tenant_agent_active ON rate_limit_configs(tenant_id, agent_id, is_active);

-- user_sessions: composite index for findByUserIdAndStatus
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_status ON user_sessions(user_id, status);

-- agent_test_results: composite index for findByExecutionIdAndTenantId
CREATE INDEX IF NOT EXISTS idx_agent_test_results_execution_tenant ON agent_test_results(execution_id, tenant_id);

-- agent_test_results: composite index for findByAgentIdAndTenantId
CREATE INDEX IF NOT EXISTS idx_agent_test_results_agent_tenant ON agent_test_results(agent_id, tenant_id);

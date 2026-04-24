-- =====================================================
-- V32: Database Constraints Enhancement
-- Round 281: Add CHECK, UNIQUE, and FK constraints
-- =====================================================

-- =====================================================
-- 1. CHECK constraints for enum-like status columns
-- =====================================================

-- agents.status: DRAFT, PENDING_APPROVAL, APPROVED, PUBLISHED, ARCHIVED
ALTER TABLE agents ADD CONSTRAINT chk_agents_status
    CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'PUBLISHED', 'ARCHIVED'));

-- agents: positive usage_count, non-negative rating
ALTER TABLE agents ADD CONSTRAINT chk_agents_usage_count
    CHECK (usage_count >= 0);
ALTER TABLE agents ADD CONSTRAINT chk_agents_rating
    CHECK (rating >= 0 AND rating <= 5.0);

-- workflow_definitions.status: DRAFT, PUBLISHED, ARCHIVED
ALTER TABLE workflow_definitions ADD CONSTRAINT chk_workflow_definitions_status
    CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'));
ALTER TABLE workflow_definitions ADD CONSTRAINT chk_workflow_definitions_version
    CHECK (version >= 1);

-- workflow_instances.status: PENDING, RUNNING, COMPLETED, FAILED, CANCELLED, SUSPENDED
ALTER TABLE workflow_instances ADD CONSTRAINT chk_workflow_instances_status
    CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED', 'SUSPENDED'));
ALTER TABLE workflow_instances ADD CONSTRAINT chk_workflow_instances_current_step
    CHECK (current_step IS NULL OR current_step >= 0);

-- agent_approvals.status: PENDING, APPROVED, REJECTED
ALTER TABLE agent_approvals ADD CONSTRAINT chk_agent_approvals_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));

-- deployment_histories.status: PENDING, DEPLOYING, SUCCESS, FAILED, ROLLED_BACK
ALTER TABLE deployment_histories ADD CONSTRAINT chk_deployment_histories_status
    CHECK (status IN ('PENDING', 'DEPLOYING', 'SUCCESS', 'FAILED', 'ROLLED_BACK'));
ALTER TABLE deployment_histories ADD CONSTRAINT chk_deployment_histories_canary_percentage
    CHECK (canary_percentage >= 0 AND canary_percentage <= 100);

-- alert_rules.severity: INFO, WARNING, CRITICAL
ALTER TABLE alert_rules ADD CONSTRAINT chk_alert_rules_severity
    CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL'));

-- alert_rules.comparison_operator: gt, lt, gte, lte, eq
ALTER TABLE alert_rules ADD CONSTRAINT chk_alert_rules_comparison_operator
    CHECK (comparison_operator IN ('gt', 'lt', 'gte', 'lte', 'eq'));

-- alert_records.status: firing, resolved
ALTER TABLE alert_records ADD CONSTRAINT chk_alert_records_status
    CHECK (status IN ('firing', 'resolved'));

-- api_call_logs.status: SUCCESS, FAILED, TIMEOUT, RATE_LIMITED, UNAUTHORIZED
ALTER TABLE api_call_logs ADD CONSTRAINT chk_api_call_logs_status
    CHECK (status IN ('SUCCESS', 'FAILED', 'TIMEOUT', 'RATE_LIMITED', 'UNAUTHORIZED'));

-- user_sessions.status: ACTIVE, EXPIRED, LOGOUT, KICKED
ALTER TABLE user_sessions ADD CONSTRAINT chk_user_sessions_status
    CHECK (status IN ('ACTIVE', 'EXPIRED', 'LOGOUT', 'KICKED'));

-- workflow_node_logs.status: PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
ALTER TABLE workflow_node_logs ADD CONSTRAINT chk_workflow_node_logs_status
    CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED'));

-- approval_chain_steps.status: PENDING, APPROVED, REJECTED, SKIPPED
ALTER TABLE approval_chain_steps ADD CONSTRAINT chk_approval_chain_steps_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SKIPPED'));

-- mcp_tool_call_logs.error_category: TIMEOUT, AUTH_FAILURE, RATE_LIMITED, INVALID_REQUEST, INTERNAL_ERROR, NONE
ALTER TABLE mcp_tool_call_logs ADD CONSTRAINT chk_mcp_tool_call_logs_error_category
    CHECK (error_category IN ('TIMEOUT', 'AUTH_FAILURE', 'RATE_LIMITED', 'INVALID_REQUEST', 'INTERNAL_ERROR', 'NONE'));

-- login_logs.login_type: LOGIN, LOGOUT, LOGIN_FAIL
ALTER TABLE login_logs ADD CONSTRAINT chk_login_logs_login_type
    CHECK (login_type IN ('LOGIN', 'LOGOUT', 'LOGIN_FAIL'));

-- login_logs.status: SUCCESS, FAIL
ALTER TABLE login_logs ADD CONSTRAINT chk_login_logs_status
    CHECK (status IN ('SUCCESS', 'FAIL'));

-- dict_types.status: active, inactive
ALTER TABLE dict_types ADD CONSTRAINT chk_dict_types_status
    CHECK (status IN ('active', 'inactive'));

-- dict_items.status: active, inactive
ALTER TABLE dict_items ADD CONSTRAINT chk_dict_items_status
    CHECK (status IN ('active', 'inactive'));

-- rate_limit_configs.limit_type: GLOBAL, AGENT, API
ALTER TABLE rate_limit_configs ADD CONSTRAINT chk_rate_limit_configs_limit_type
    CHECK (limit_type IN ('GLOBAL', 'AGENT', 'API'));

-- rate_limit_configs: positive rate limits
ALTER TABLE rate_limit_configs ADD CONSTRAINT chk_rate_limit_configs_rps
    CHECK (requests_per_second > 0);
ALTER TABLE rate_limit_configs ADD CONSTRAINT chk_rate_limit_configs_rpm
    CHECK (requests_per_minute > 0);
ALTER TABLE rate_limit_configs ADD CONSTRAINT chk_rate_limit_configs_rph
    CHECK (requests_per_hour > 0);
ALTER TABLE rate_limit_configs ADD CONSTRAINT chk_rate_limit_configs_rpd
    CHECK (requests_per_day > 0);
ALTER TABLE rate_limit_configs ADD CONSTRAINT chk_rate_limit_configs_burst
    CHECK (burst_capacity > 0);

-- mcp_tools.health_status: HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN
ALTER TABLE mcp_tools ADD CONSTRAINT chk_mcp_tools_health_status
    CHECK (health_status IN ('HEALTHY', 'DEGRADED', 'UNHEALTHY', 'UNKNOWN'));
ALTER TABLE mcp_tools ADD CONSTRAINT chk_mcp_tools_consecutive_failures
    CHECK (consecutive_failures >= 0);
ALTER TABLE mcp_tools ADD CONSTRAINT chk_mcp_tools_avg_response_time
    CHECK (avg_response_time >= 0);

-- agent_evolution_experiences: non-negative usage_count
ALTER TABLE agent_evolution_experiences ADD CONSTRAINT chk_evolution_experiences_usage_count
    CHECK (usage_count >= 0);
ALTER TABLE agent_evolution_experiences ADD CONSTRAINT chk_evolution_experiences_effectiveness
    CHECK (effectiveness_score IS NULL OR (effectiveness_score >= 0 AND effectiveness_score <= 100));

-- agent_evolution_suggestions: non-negative priority
ALTER TABLE agent_evolution_suggestions ADD CONSTRAINT chk_evolution_suggestions_priority
    CHECK (priority >= 0);

-- agent_evolution_reflections: score ranges
ALTER TABLE agent_evolution_reflections ADD CONSTRAINT chk_evolution_reflections_performance
    CHECK (performance_score IS NULL OR (performance_score >= 0 AND performance_score <= 100));
ALTER TABLE agent_evolution_reflections ADD CONSTRAINT chk_evolution_reflections_accuracy
    CHECK (accuracy_score IS NULL OR (accuracy_score >= 0 AND accuracy_score <= 100));
ALTER TABLE agent_evolution_reflections ADD CONSTRAINT chk_evolution_reflections_efficiency
    CHECK (efficiency_score IS NULL OR (efficiency_score >= 0 AND efficiency_score <= 100));
ALTER TABLE agent_evolution_reflections ADD CONSTRAINT chk_evolution_reflections_satisfaction
    CHECK (user_satisfaction_score IS NULL OR (user_satisfaction_score >= 0 AND user_satisfaction_score <= 100));

-- tenants: positive quota limits
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_max_agents
    CHECK (max_agents > 0);
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_max_api_calls
    CHECK (max_api_calls_per_day > 0);
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_max_tokens
    CHECK (max_tokens_per_day > 0);
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_max_mcp_calls
    CHECK (max_mcp_calls_per_day > 0);
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_max_storage
    CHECK (max_storage_mb > 0);
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_max_workflows
    CHECK (max_workflows > 0);

-- tenants: non-negative usage counters
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_used_agents
    CHECK (used_agents >= 0);
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_used_api_calls
    CHECK (used_api_calls_today >= 0);
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_used_tokens
    CHECK (used_tokens_today >= 0);
ALTER TABLE tenants ADD CONSTRAINT chk_tenants_used_workflows
    CHECK (used_workflows >= 0);

-- users: non-negative failed login attempts
ALTER TABLE users ADD CONSTRAINT chk_users_failed_login_attempts
    CHECK (failed_login_attempts >= 0);

-- api_interfaces: valid HTTP methods
ALTER TABLE api_interfaces ADD CONSTRAINT chk_api_interfaces_method
    CHECK (method IN ('GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'));

-- =====================================================
-- 2. UNIQUE constraints for natural keys
-- =====================================================

-- tenant + agent name uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS uk_agents_tenant_name
    ON agents(tenant_id, name) WHERE deleted = false;

-- tenant + workflow definition name uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS uk_workflow_definitions_tenant_name
    ON workflow_definitions(tenant_id, name) WHERE deleted = false;

-- tenant + role name uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS uk_roles_tenant_name
    ON roles(tenant_id, name) WHERE deleted = false;

-- tenant + permission name uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS uk_permissions_tenant_name
    ON permissions(tenant_id, name) WHERE deleted = false;

-- tenant + mcp_tool_code uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS uk_mcp_tools_tenant_code
    ON mcp_tools(tenant_id, tool_code) WHERE deleted = false;

-- tenant + approval_chain name uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS uk_approval_chains_tenant_name
    ON approval_chains(tenant_id, name) WHERE deleted = false;

-- user + role uniqueness (prevent duplicate role assignment)
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_roles_user_role
    ON user_roles(user_id, role_id);

-- role + permission uniqueness (prevent duplicate permission assignment)
CREATE UNIQUE INDEX IF NOT EXISTS uk_role_permissions_role_permission
    ON role_permissions(role_id, permission_id);

-- agent + api_interface path+method uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS uk_api_interfaces_agent_path_method
    ON api_interfaces(agent_id, path, method) WHERE deleted = false;

-- =====================================================
-- 3. FOREIGN KEY constraints with CASCADE/SET NULL
-- =====================================================

-- users -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_users_tenant') THEN
        ALTER TABLE users ADD CONSTRAINT fk_users_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE SET NULL;
    END IF;
END $$;

-- agents -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agents_tenant') THEN
        ALTER TABLE agents ADD CONSTRAINT fk_agents_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_versions -> agents
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_versions_agent') THEN
        ALTER TABLE agent_versions ADD CONSTRAINT fk_agent_versions_agent
            FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_versions -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_versions_tenant') THEN
        ALTER TABLE agent_versions ADD CONSTRAINT fk_agent_versions_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- api_interfaces -> agents
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_api_interfaces_agent') THEN
        ALTER TABLE api_interfaces ADD CONSTRAINT fk_api_interfaces_agent
            FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
    END IF;
END $$;

-- api_interfaces -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_api_interfaces_tenant') THEN
        ALTER TABLE api_interfaces ADD CONSTRAINT fk_api_interfaces_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- workflow_definitions -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_workflow_definitions_tenant') THEN
        ALTER TABLE workflow_definitions ADD CONSTRAINT fk_workflow_definitions_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- workflow_instances -> workflow_definitions
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_workflow_instances_definition') THEN
        ALTER TABLE workflow_instances ADD CONSTRAINT fk_workflow_instances_definition
            FOREIGN KEY (workflow_definition_id) REFERENCES workflow_definitions(id) ON DELETE CASCADE;
    END IF;
END $$;

-- workflow_instances -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_workflow_instances_tenant') THEN
        ALTER TABLE workflow_instances ADD CONSTRAINT fk_workflow_instances_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- workflow_node_logs -> workflow_instances
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_workflow_node_logs_instance') THEN
        ALTER TABLE workflow_node_logs ADD CONSTRAINT fk_workflow_node_logs_instance
            FOREIGN KEY (instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_approvals -> agents
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_approvals_agent') THEN
        ALTER TABLE agent_approvals ADD CONSTRAINT fk_agent_approvals_agent
            FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_approvals -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_approvals_tenant') THEN
        ALTER TABLE agent_approvals ADD CONSTRAINT fk_agent_approvals_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_approvals -> agent_versions
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_approvals_version') THEN
        ALTER TABLE agent_approvals ADD CONSTRAINT fk_agent_approvals_version
            FOREIGN KEY (agent_version_id) REFERENCES agent_versions(id) ON DELETE CASCADE;
    END IF;
END $$;

-- deployment_histories -> agents
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_deployment_histories_agent') THEN
        ALTER TABLE deployment_histories ADD CONSTRAINT fk_deployment_histories_agent
            FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
    END IF;
END $$;

-- deployment_histories -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_deployment_histories_tenant') THEN
        ALTER TABLE deployment_histories ADD CONSTRAINT fk_deployment_histories_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- deployment_histories -> agent_versions
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_deployment_histories_version') THEN
        ALTER TABLE deployment_histories ADD CONSTRAINT fk_deployment_histories_version
            FOREIGN KEY (agent_version_id) REFERENCES agent_versions(id) ON DELETE CASCADE;
    END IF;
END $$;

-- mcp_tools -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_mcp_tools_tenant') THEN
        ALTER TABLE mcp_tools ADD CONSTRAINT fk_mcp_tools_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- mcp_tool_call_logs -> mcp_tools
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_mcp_tool_call_logs_tool') THEN
        ALTER TABLE mcp_tool_call_logs ADD CONSTRAINT fk_mcp_tool_call_logs_tool
            FOREIGN KEY (mcp_tool_id) REFERENCES mcp_tools(id) ON DELETE CASCADE;
    END IF;
END $$;

-- mcp_tool_call_logs -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_mcp_tool_call_logs_tenant') THEN
        ALTER TABLE mcp_tool_call_logs ADD CONSTRAINT fk_mcp_tool_call_logs_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- mcp_tool_call_logs -> api_call_logs
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_mcp_tool_call_logs_api_call') THEN
        ALTER TABLE mcp_tool_call_logs ADD CONSTRAINT fk_mcp_tool_call_logs_api_call
            FOREIGN KEY (api_call_log_id) REFERENCES api_call_logs(id) ON DELETE SET NULL;
    END IF;
END $$;

-- user_roles -> users
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_roles_user') THEN
        ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- user_roles -> roles
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_roles_role') THEN
        ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_role
            FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE;
    END IF;
END $$;

-- role_permissions -> roles
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_role_permissions_role') THEN
        ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_role
            FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE;
    END IF;
END $$;

-- role_permissions -> permissions
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_role_permissions_permission') THEN
        ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_permission
            FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE;
    END IF;
END $$;

-- permissions -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_permissions_tenant') THEN
        ALTER TABLE permissions ADD CONSTRAINT fk_permissions_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- roles -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_roles_tenant') THEN
        ALTER TABLE roles ADD CONSTRAINT fk_roles_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- rate_limit_configs -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_rate_limit_configs_tenant') THEN
        ALTER TABLE rate_limit_configs ADD CONSTRAINT fk_rate_limit_configs_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- rate_limit_configs -> agents
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_rate_limit_configs_agent') THEN
        ALTER TABLE rate_limit_configs ADD CONSTRAINT fk_rate_limit_configs_agent
            FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE SET NULL;
    END IF;
END $$;

-- rate_limit_configs -> api_interfaces
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_rate_limit_configs_api_interface') THEN
        ALTER TABLE rate_limit_configs ADD CONSTRAINT fk_rate_limit_configs_api_interface
            FOREIGN KEY (api_interface_id) REFERENCES api_interfaces(id) ON DELETE SET NULL;
    END IF;
END $$;

-- alert_rules -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_alert_rules_tenant') THEN
        ALTER TABLE alert_rules ADD CONSTRAINT fk_alert_rules_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- alert_records -> alert_rules
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_alert_records_rule') THEN
        ALTER TABLE alert_records ADD CONSTRAINT fk_alert_records_rule
            FOREIGN KEY (rule_id) REFERENCES alert_rules(id) ON DELETE SET NULL;
    END IF;
END $$;

-- alert_records -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_alert_records_tenant') THEN
        ALTER TABLE alert_records ADD CONSTRAINT fk_alert_records_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- approval_chains -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_approval_chains_tenant') THEN
        ALTER TABLE approval_chains ADD CONSTRAINT fk_approval_chains_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- approval_chain_steps -> approval_chains
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_approval_chain_steps_chain') THEN
        ALTER TABLE approval_chain_steps ADD CONSTRAINT fk_approval_chain_steps_chain
            FOREIGN KEY (chain_id) REFERENCES approval_chains(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_evolution_experiences -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_evolution_experiences_tenant') THEN
        ALTER TABLE agent_evolution_experiences ADD CONSTRAINT fk_evolution_experiences_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_evolution_experiences -> agents
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_evolution_experiences_agent') THEN
        ALTER TABLE agent_evolution_experiences ADD CONSTRAINT fk_evolution_experiences_agent
            FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_evolution_suggestions -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_evolution_suggestions_tenant') THEN
        ALTER TABLE agent_evolution_suggestions ADD CONSTRAINT fk_evolution_suggestions_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_evolution_suggestions -> agents
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_evolution_suggestions_agent') THEN
        ALTER TABLE agent_evolution_suggestions ADD CONSTRAINT fk_evolution_suggestions_agent
            FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_evolution_reflections -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_evolution_reflections_tenant') THEN
        ALTER TABLE agent_evolution_reflections ADD CONSTRAINT fk_evolution_reflections_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_evolution_reflections -> agents
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_evolution_reflections_agent') THEN
        ALTER TABLE agent_evolution_reflections ADD CONSTRAINT fk_evolution_reflections_agent
            FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_test_cases -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_test_cases_tenant') THEN
        ALTER TABLE agent_test_cases ADD CONSTRAINT fk_agent_test_cases_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_test_cases -> agents
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_test_cases_agent') THEN
        ALTER TABLE agent_test_cases ADD CONSTRAINT fk_agent_test_cases_agent
            FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_test_executions -> agent_test_cases
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_test_executions_test_case') THEN
        ALTER TABLE agent_test_executions ADD CONSTRAINT fk_agent_test_executions_test_case
            FOREIGN KEY (test_case_id) REFERENCES agent_test_cases(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_test_executions -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_test_executions_tenant') THEN
        ALTER TABLE agent_test_executions ADD CONSTRAINT fk_agent_test_executions_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_test_results -> agent_test_executions
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_test_results_execution') THEN
        ALTER TABLE agent_test_results ADD CONSTRAINT fk_agent_test_results_execution
            FOREIGN KEY (execution_id) REFERENCES agent_test_executions(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_test_results -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_test_results_tenant') THEN
        ALTER TABLE agent_test_results ADD CONSTRAINT fk_agent_test_results_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- execution_history -> agents
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_execution_history_agent') THEN
        ALTER TABLE execution_history ADD CONSTRAINT fk_execution_history_agent
            FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
    END IF;
END $$;

-- execution_history -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_execution_history_tenant') THEN
        ALTER TABLE execution_history ADD CONSTRAINT fk_execution_history_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- execution_history -> users
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_execution_history_user') THEN
        ALTER TABLE execution_history ADD CONSTRAINT fk_execution_history_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- user_sessions -> users
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_sessions_user') THEN
        ALTER TABLE user_sessions ADD CONSTRAINT fk_user_sessions_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- api_call_logs -> agents
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_api_call_logs_agent') THEN
        ALTER TABLE api_call_logs ADD CONSTRAINT fk_api_call_logs_agent
            FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
    END IF;
END $$;

-- api_call_logs -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_api_call_logs_tenant') THEN
        ALTER TABLE api_call_logs ADD CONSTRAINT fk_api_call_logs_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- api_call_logs -> users
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_api_call_logs_user') THEN
        ALTER TABLE api_call_logs ADD CONSTRAINT fk_api_call_logs_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- api_call_logs -> api_interfaces
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_api_call_logs_api_interface') THEN
        ALTER TABLE api_call_logs ADD CONSTRAINT fk_api_call_logs_api_interface
            FOREIGN KEY (api_interface_id) REFERENCES api_interfaces(id) ON DELETE SET NULL;
    END IF;
END $$;

-- api_call_audit_logs -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_api_call_audit_logs_tenant') THEN
        ALTER TABLE api_call_audit_logs ADD CONSTRAINT fk_api_call_audit_logs_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE SET NULL;
    END IF;
END $$;

-- api_call_audit_logs -> users
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_api_call_audit_logs_user') THEN
        ALTER TABLE api_call_audit_logs ADD CONSTRAINT fk_api_call_audit_logs_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- password_histories -> users
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_password_histories_user') THEN
        ALTER TABLE password_histories ADD CONSTRAINT fk_password_histories_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_memories -> agents
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_memories_agent') THEN
        ALTER TABLE agent_memories ADD CONSTRAINT fk_agent_memories_agent
            FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
    END IF;
END $$;

-- agent_memories -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_memories_tenant') THEN
        ALTER TABLE agent_memories ADD CONSTRAINT fk_agent_memories_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- login_logs -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_login_logs_tenant') THEN
        ALTER TABLE login_logs ADD CONSTRAINT fk_login_logs_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE SET NULL;
    END IF;
END $$;

-- login_logs -> users
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_login_logs_user') THEN
        ALTER TABLE login_logs ADD CONSTRAINT fk_login_logs_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- permission_matrix -> roles
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_permission_matrix_role') THEN
        ALTER TABLE permission_matrix ADD CONSTRAINT fk_permission_matrix_role
            FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE;
    END IF;
END $$;

-- permission_matrix -> tenants
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_permission_matrix_tenant') THEN
        ALTER TABLE permission_matrix ADD CONSTRAINT fk_permission_matrix_tenant
            FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
    END IF;
END $$;

-- =====================================================
-- 4. Additional partial indexes for remaining soft-delete tables
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_rate_limit_configs_deleted
    ON rate_limit_configs(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_agent_evolution_experiences_deleted
    ON agent_evolution_experiences(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_agent_evolution_suggestions_deleted
    ON agent_evolution_suggestions(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_agent_evolution_reflections_deleted
    ON agent_evolution_reflections(deleted) WHERE deleted = false;

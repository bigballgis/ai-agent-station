-- =====================================================
-- V30: Add soft delete support (deleted column) to all BaseEntity tables
-- Round 262: Soft Delete Enhancement
-- =====================================================

-- Add deleted column to all tables that extend BaseEntity
-- Default is false, NOT NULL constraint added after backfill

ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE agents ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE mcp_tools ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE workflow_definitions ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE api_interfaces ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE deployment_histories ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE approval_chains ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE dict_types ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE agent_evolution_suggestions ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE agent_evolution_reflections ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE agent_approvals ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE rate_limit_configs ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE agent_test_cases ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE agent_test_results ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE agent_versions ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE agent_evolution_experiences ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE agent_test_executions ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;

-- Add indexes for deleted column on high-traffic tables
CREATE INDEX IF NOT EXISTS idx_agents_deleted ON agents(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_users_deleted ON users(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_tenants_deleted ON tenants(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_workflow_definitions_deleted ON workflow_definitions(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_api_interfaces_deleted ON api_interfaces(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_agent_approvals_deleted ON agent_approvals(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_agent_versions_deleted ON agent_versions(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_roles_deleted ON roles(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_permissions_deleted ON permissions(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_mcp_tools_deleted ON mcp_tools(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_deployment_histories_deleted ON deployment_histories(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_approval_chains_deleted ON approval_chains(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_rate_limit_configs_deleted ON rate_limit_configs(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_dict_types_deleted ON dict_types(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_agent_test_cases_deleted ON agent_test_cases(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_agent_test_results_deleted ON agent_test_results(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_agent_test_executions_deleted ON agent_test_executions(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_agent_evolution_suggestions_deleted ON agent_evolution_suggestions(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_agent_evolution_reflections_deleted ON agent_evolution_reflections(deleted) WHERE deleted = false;
CREATE INDEX IF NOT EXISTS idx_agent_evolution_experiences_deleted ON agent_evolution_experiences(deleted) WHERE deleted = false;

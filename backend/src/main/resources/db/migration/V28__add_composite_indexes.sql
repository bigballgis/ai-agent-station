-- =====================================================
-- V28: Add composite indexes for ORDER BY query optimization
-- =====================================================

-- deployment_history: composite index for findSuccessfulDeployments (tenant_id + agent_id + deployed_at DESC)
CREATE INDEX IF NOT EXISTS idx_deployment_history_tenant_agent_deployed
    ON deployment_history(tenant_id, agent_id, deployed_at DESC);

-- deployment_history: composite index for findLatestDeployment (tenant_id + agent_id + created_at DESC)
CREATE INDEX IF NOT EXISTS idx_deployment_history_tenant_agent_created
    ON deployment_history(tenant_id, agent_id, created_at DESC);

-- agent_approvals: composite index for findLatestByAgentId (tenant_id + agent_id + submitted_at DESC)
CREATE INDEX IF NOT EXISTS idx_agent_approvals_tenant_agent_submitted
    ON agent_approvals(tenant_id, agent_id, submitted_at DESC);

-- agent_test_results: composite index for pass rate queries (agent_id + status)
CREATE INDEX IF NOT EXISTS idx_agent_test_results_agent_status
    ON agent_test_results(agent_id, status);

-- agent_test_results: composite index for pass rate by test case queries (test_case_id + status)
CREATE INDEX IF NOT EXISTS idx_agent_test_results_testcase_status
    ON agent_test_results(test_case_id, status);

-- agent_memories: composite index for searchMemories query (agent_id + tenant_id)
CREATE INDEX IF NOT EXISTS idx_agent_memories_agent_tenant
    ON agent_memories(agent_id, tenant_id);

-- user_sessions: composite index for findByUserIdAndStatus (user_id + status)
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_status
    ON user_sessions(user_id, status);

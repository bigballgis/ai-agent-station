-- V9: 合并两套租户表结构 + 修复外键引用

-- =============================================
-- 1. 合并 tenant 和 tenants 表
-- =============================================

-- 检查旧 tenant 表是否存在，如果存在则迁移数据
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'tenant') THEN
        -- 将旧 tenant 表数据迁移到 tenants 表（如果 tenants 表中不存在对应记录）
        INSERT INTO tenants (name, description, schema_name, is_active, api_key, api_secret, created_at, updated_at)
        SELECT
            tenant_name,
            COALESCE(tenant_desc, ''),
            tenant_schema,
            CASE WHEN status = 1 THEN true ELSE false END,
            COALESCE(api_key, ''),
            COALESCE(api_secret, ''),
            COALESCE(created_at, CURRENT_TIMESTAMP),
            COALESCE(updated_at, CURRENT_TIMESTAMP)
        FROM tenant t
        WHERE NOT EXISTS (
            SELECT 1 FROM tenants ts WHERE ts.schema_name = t.tenant_schema
        );

        RAISE NOTICE 'Migrated data from tenant to tenants table';
    END IF;
END $$;

-- 删除旧的 tenant 表（如果存在）
DROP TABLE IF EXISTS tenant CASCADE;

-- =============================================
-- 2. 修复 V6 中的外键引用错误
-- =============================================

-- agent_test_cases 表的外键引用修复（agent → agents）
DO $$
BEGIN
    -- 检查约束是否存在
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_agent_test_cases_agent'
    ) THEN
        ALTER TABLE agent_test_cases DROP CONSTRAINT fk_agent_test_cases_agent;
        RAISE NOTICE 'Dropped old fk_agent_test_cases_agent constraint';
    END IF;

    -- 重新创建正确的约束（引用 agents 表）
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'agent_test_cases' AND column_name = 'agent_id'
    ) THEN
        ALTER TABLE agent_test_cases
            ADD CONSTRAINT fk_agent_test_cases_agent
            FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
        RAISE NOTICE 'Created correct fk_agent_test_cases_agent constraint';
    END IF;
END $$;

-- =============================================
-- 3. users 表 username 组合唯一约束
-- =============================================

DO $$
BEGIN
    -- 删除旧的 username 唯一约束
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'uk_users_username'
    ) THEN
        ALTER TABLE users DROP CONSTRAINT uk_users_username;
        RAISE NOTICE 'Dropped old uk_users_username constraint';
    END IF;

    -- 创建新的组合唯一约束 (username, tenant_id)
    -- 注意：tenant_id 可能为 NULL，PostgreSQL 中 NULL != NULL
    -- 使用 COALESCE 处理 NULL 值
    ALTER TABLE users
        ADD CONSTRAINT uk_users_username_tenant
        UNIQUE (username, COALESCE(tenant_id, 0));
    RAISE NOTICE 'Created uk_users_username_tenant constraint';
END $$;

-- =============================================
-- 4. 添加性能优化索引
-- =============================================

-- agent_versions: (agent_id, version_number) 复合索引
CREATE INDEX IF NOT EXISTS idx_agent_versions_agent_version
    ON agent_versions(agent_id, version_number);

-- agent_approvals: (agent_id, status) 复合索引
CREATE INDEX IF NOT EXISTS idx_agent_approvals_agent_status
    ON agent_approvals(agent_id, status);

-- system_logs: (module, created_at) 复合索引
CREATE INDEX IF NOT EXISTS idx_system_logs_module_created
    ON system_logs(module, created_at);

-- api_call_logs: (agent_id, created_at) 复合索引
CREATE INDEX IF NOT EXISTS idx_api_call_logs_agent_created
    ON api_call_logs(agent_id, created_at);

-- agent_test_executions: (agent_id, status) 复合索引
CREATE INDEX IF NOT EXISTS idx_test_executions_agent_status
    ON agent_test_executions(agent_id, status);

-- agent_test_results: (execution_id) 索引
CREATE INDEX IF NOT EXISTS idx_test_results_execution
    ON agent_test_results(execution_id);

-- deployment_history: (agent_id, status) 复合索引
CREATE INDEX IF NOT EXISTS idx_deployment_history_agent_status
    ON deployment_history(agent_id, status);

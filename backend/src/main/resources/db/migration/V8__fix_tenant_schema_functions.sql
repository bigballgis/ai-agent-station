-- V8: 修复租户 schema 函数重复定义问题
-- 创建统一的、最新的 create_tenant_schema 函数

CREATE OR REPLACE FUNCTION create_tenant_schema(p_schema_name TEXT)
RETURNS void AS $$
DECLARE
    v_full_schema_name TEXT;
BEGIN
    v_full_schema_name := 't_' || p_schema_name;

    -- 检查 schema 是否已存在
    IF NOT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = v_full_schema_name) THEN
        EXECUTE format('CREATE SCHEMA %I', v_full_schema_name);
        RAISE NOTICE 'Created schema: %', v_full_schema_name;
    END IF;

    -- 创建 agent_test_cases 表（如果不存在）
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = v_full_schema_name AND table_name = 'agent_test_cases') THEN
        EXECUTE format('
            CREATE TABLE %I.agent_test_cases (
                id BIGSERIAL PRIMARY KEY,
                agent_id BIGINT NOT NULL,
                name VARCHAR(200) NOT NULL,
                description TEXT,
                input_type VARCHAR(50) DEFAULT ''text'',
                input_data JSONB,
                expected_output JSONB,
                priority INTEGER DEFAULT 0,
                status VARCHAR(20) DEFAULT ''active'',
                tags JSONB DEFAULT ''[]'',
                created_by BIGINT,
                updated_by BIGINT,
                tenant_id BIGINT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )', v_full_schema_name);
    END IF;

    -- 创建 agent_test_executions 表（如果不存在）
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = v_full_schema_name AND table_name = 'agent_test_executions') THEN
        EXECUTE format('
            CREATE TABLE %I.agent_test_executions (
                id BIGSERIAL PRIMARY KEY,
                test_case_id BIGINT NOT NULL,
                agent_id BIGINT NOT NULL,
                status VARCHAR(20) DEFAULT ''pending'',
                input_data JSONB,
                actual_output JSONB,
                execution_time INTEGER,
                error_message TEXT,
                started_at TIMESTAMP,
                completed_at TIMESTAMP,
                created_by BIGINT,
                tenant_id BIGINT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )', v_full_schema_name);
    END IF;

    -- 创建 agent_test_results 表（如果不存在）
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = v_full_schema_name AND table_name = 'agent_test_results') THEN
        EXECUTE format('
            CREATE TABLE %I.agent_test_results (
                id BIGSERIAL PRIMARY KEY,
                execution_id BIGINT NOT NULL,
                test_case_id BIGINT NOT NULL,
                agent_id BIGINT NOT NULL,
                status VARCHAR(20) DEFAULT ''pending'',
                passed BOOLEAN,
                score DECIMAL(5,2),
                actual_output JSONB,
                expected_output JSONB,
                error_message TEXT,
                execution_time INTEGER,
                created_by BIGINT,
                tenant_id BIGINT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )', v_full_schema_name);
    END IF;

    -- 创建 agent_evolution_experiences 表（如果不存在）
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = v_full_schema_name AND table_name = 'agent_evolution_experiences') THEN
        EXECUTE format('
            CREATE TABLE %I.agent_evolution_experiences (
                id BIGSERIAL PRIMARY KEY,
                agent_id BIGINT NOT NULL,
                experience_type VARCHAR(50) NOT NULL,
                content JSONB NOT NULL,
                summary TEXT,
                tags JSONB DEFAULT ''[]'',
                source VARCHAR(50),
                status INTEGER DEFAULT 1,
                usage_count INTEGER DEFAULT 0,
                success_rate DECIMAL(5,2),
                last_used_at TIMESTAMP,
                created_by BIGINT,
                tenant_id BIGINT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )', v_full_schema_name);
    END IF;

    -- 创建 agent_evolution_reflections 表（如果不存在）
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = v_full_schema_name AND table_name = 'agent_evolution_reflections') THEN
        EXECUTE format('
            CREATE TABLE %I.agent_evolution_reflections (
                id BIGSERIAL PRIMARY KEY,
                agent_id BIGINT NOT NULL,
                reflection_type VARCHAR(50) NOT NULL,
                content TEXT NOT NULL,
                metrics JSONB,
                insights JSONB DEFAULT ''[]'',
                action_items JSONB DEFAULT ''[]'',
                created_by BIGINT,
                tenant_id BIGINT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )', v_full_schema_name);
    END IF;

    -- 创建 agent_evolution_suggestions 表（如果不存在）
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = v_full_schema_name AND table_name = 'agent_evolution_suggestions') THEN
        EXECUTE format('
            CREATE TABLE %I.agent_evolution_suggestions (
                id BIGSERIAL PRIMARY KEY,
                agent_id BIGINT NOT NULL,
                suggestion_type VARCHAR(50) NOT NULL,
                title VARCHAR(200) NOT NULL,
                description TEXT,
                priority INTEGER DEFAULT 3,
                status VARCHAR(20) DEFAULT ''pending'',
                implementation JSONB,
                expected_impact JSONB,
                applied BOOLEAN DEFAULT false,
                applied_at TIMESTAMP,
                result JSONB,
                created_by BIGINT,
                tenant_id BIGINT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )', v_full_schema_name);
    END IF;

    RAISE NOTICE 'Schema % set up completed', v_full_schema_name;
END;
$$ LANGUAGE plpgsql;

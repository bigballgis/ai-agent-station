-- V10: 添加 Agent 记忆管理表

CREATE TABLE IF NOT EXISTS agent_memories (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    session_id VARCHAR(100),
    memory_type VARCHAR(20) NOT NULL DEFAULT 'SHORT_TERM',
    content TEXT NOT NULL,
    summary TEXT,
    tags VARCHAR(500),
    importance DECIMAL(3,2) DEFAULT 0.50,
    access_count INTEGER DEFAULT 0,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    tenant_id BIGINT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_memories_agent_tenant ON agent_memories(agent_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_memories_session ON agent_memories(agent_id, session_id);
CREATE INDEX IF NOT EXISTS idx_memories_type ON agent_memories(agent_id, memory_type);
CREATE INDEX IF NOT EXISTS idx_memories_expires ON agent_memories(expires_at) WHERE expires_at IS NOT NULL;

-- Agent 表添加缺失字段
ALTER TABLE agents ADD COLUMN IF NOT EXISTS icon VARCHAR(100);
ALTER TABLE agents ADD COLUMN IF NOT EXISTS language VARCHAR(20) DEFAULT 'zh-CN';
ALTER TABLE agents ADD COLUMN IF NOT EXISTS category VARCHAR(100);
ALTER TABLE agents ADD COLUMN IF NOT EXISTS tags JSONB DEFAULT '[]';

-- V27: 增强 Agent 模板系统
-- 添加 is_template, rating, usage_count 字段到 agents 表

ALTER TABLE agents ADD COLUMN IF NOT EXISTS is_template BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE agents ADD COLUMN IF NOT EXISTS rating NUMERIC(3, 2) DEFAULT 0.0;
ALTER TABLE agents ADD COLUMN IF NOT EXISTS usage_count INTEGER NOT NULL DEFAULT 0;

-- 模板搜索索引
CREATE INDEX IF NOT EXISTS idx_agents_is_template ON agents(is_template);
CREATE INDEX IF NOT EXISTS idx_agents_category ON agents(category);

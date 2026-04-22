-- Agent表
CREATE TABLE IF NOT EXISTS agents (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(200) NOT NULL,
  description VARCHAR(500),
  config JSONB NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Agent版本表
CREATE TABLE IF NOT EXISTS agent_versions (
  id BIGSERIAL PRIMARY KEY,
  agent_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  version_number INT NOT NULL,
  config JSONB NOT NULL,
  change_log VARCHAR(500),
  created_by BIGINT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_agents_tenant_id ON agents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_versions_agent_id ON agent_versions(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_versions_tenant_id ON agent_versions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_versions_version_number ON agent_versions(version_number DESC);

-- 外键约束
ALTER TABLE agents ADD CONSTRAINT fk_agents_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE agent_versions ADD CONSTRAINT fk_agent_versions_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
ALTER TABLE agent_versions ADD CONSTRAINT fk_agent_versions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

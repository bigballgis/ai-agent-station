-- Agent状态枚举类型
CREATE TYPE agent_status AS ENUM ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'PUBLISHED', 'ARCHIVED');

-- 审批状态枚举类型
CREATE TYPE approval_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

-- 发布状态枚举类型
CREATE TYPE deployment_status AS ENUM ('PENDING', 'DEPLOYING', 'SUCCESS', 'FAILED', 'ROLLED_BACK');

-- 审批记录表
CREATE TABLE IF NOT EXISTS agent_approvals (
  id BIGSERIAL PRIMARY KEY,
  agent_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  agent_version_id BIGINT NOT NULL,
  submitter_id BIGINT NOT NULL,
  approver_id BIGINT,
  status approval_status NOT NULL DEFAULT 'PENDING',
  remark VARCHAR(500),
  approval_remark VARCHAR(500),
  submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  approved_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- API接口表
CREATE TABLE IF NOT EXISTS api_interfaces (
  id BIGSERIAL PRIMARY KEY,
  agent_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  version_id BIGINT,
  path VARCHAR(200) NOT NULL,
  method VARCHAR(10) NOT NULL,
  description VARCHAR(500),
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 发布历史表
CREATE TABLE IF NOT EXISTS deployment_history (
  id BIGSERIAL PRIMARY KEY,
  agent_id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  agent_version_id BIGINT NOT NULL,
  approver_id BIGINT,
  deployer_id BIGINT NOT NULL,
  status deployment_status NOT NULL DEFAULT 'PENDING',
  version VARCHAR(50) NOT NULL,
  is_canary BOOLEAN NOT NULL DEFAULT false,
  canary_percentage INTEGER DEFAULT 0,
  rollback_from_id BIGINT,
  deployed_at TIMESTAMP,
  rollback_at TIMESTAMP,
  remark VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 更新agents表，添加状态字段
ALTER TABLE agents ADD COLUMN IF NOT EXISTS status agent_status NOT NULL DEFAULT 'DRAFT';
ALTER TABLE agents ADD COLUMN IF NOT EXISTS published_version_id BIGINT;
ALTER TABLE agents ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;

-- 更新agent_versions表，添加语义化版本号字段
ALTER TABLE agent_versions ADD COLUMN IF NOT EXISTS semantic_version VARCHAR(50);

-- 索引
CREATE INDEX IF NOT EXISTS idx_agent_approvals_agent_id ON agent_approvals(agent_id);
CREATE INDEX IF NOT EXISTS idx_agent_approvals_tenant_id ON agent_approvals(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_approvals_status ON agent_approvals(status);
CREATE INDEX IF NOT EXISTS idx_agent_approvals_submitted_at ON agent_approvals(submitted_at DESC);

CREATE INDEX IF NOT EXISTS idx_api_interfaces_agent_id ON api_interfaces(agent_id);
CREATE INDEX IF NOT EXISTS idx_api_interfaces_tenant_id ON api_interfaces(tenant_id);
CREATE INDEX IF NOT EXISTS idx_api_interfaces_path ON api_interfaces(path);

CREATE INDEX IF NOT EXISTS idx_deployment_history_agent_id ON deployment_history(agent_id);
CREATE INDEX IF NOT EXISTS idx_deployment_history_tenant_id ON deployment_history(tenant_id);
CREATE INDEX IF NOT EXISTS idx_deployment_history_status ON deployment_history(status);
CREATE INDEX IF NOT EXISTS idx_deployment_history_created_at ON deployment_history(created_at DESC);

-- 外键约束
ALTER TABLE agent_approvals ADD CONSTRAINT fk_agent_approvals_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
ALTER TABLE agent_approvals ADD CONSTRAINT fk_agent_approvals_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE agent_approvals ADD CONSTRAINT fk_agent_approvals_agent_version FOREIGN KEY (agent_version_id) REFERENCES agent_versions(id) ON DELETE CASCADE;

ALTER TABLE api_interfaces ADD CONSTRAINT fk_api_interfaces_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
ALTER TABLE api_interfaces ADD CONSTRAINT fk_api_interfaces_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE api_interfaces ADD CONSTRAINT fk_api_interfaces_version FOREIGN KEY (version_id) REFERENCES agent_versions(id) ON DELETE SET NULL;

ALTER TABLE deployment_history ADD CONSTRAINT fk_deployment_history_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
ALTER TABLE deployment_history ADD CONSTRAINT fk_deployment_history_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);
ALTER TABLE deployment_history ADD CONSTRAINT fk_deployment_history_agent_version FOREIGN KEY (agent_version_id) REFERENCES agent_versions(id) ON DELETE CASCADE;
ALTER TABLE deployment_history ADD CONSTRAINT fk_deployment_history_rollback_from FOREIGN KEY (rollback_from_id) REFERENCES deployment_history(id) ON DELETE SET NULL;

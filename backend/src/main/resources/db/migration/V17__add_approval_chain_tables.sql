-- V17: 审批链表
-- 审批链定义表
CREATE TABLE approval_chains (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    tenant_id BIGINT NOT NULL,
    steps JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_approval_chain_tenant ON approval_chains(tenant_id);
CREATE INDEX idx_approval_chain_status ON approval_chains(status);

-- 审批链步骤记录表
CREATE TABLE approval_chain_steps (
    id BIGSERIAL PRIMARY KEY,
    chain_id BIGINT NOT NULL,
    approval_id BIGINT NOT NULL,
    step_level INT NOT NULL,
    step_name VARCHAR(200),
    approver_id BIGINT,
    approver_name VARCHAR(100),
    approver_type VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    comment VARCHAR(1000),
    acted_at TIMESTAMP
);

CREATE INDEX idx_chain_step_chain ON approval_chain_steps(chain_id);
CREATE INDEX idx_chain_step_approval ON approval_chain_steps(approval_id);
CREATE INDEX idx_chain_step_status ON approval_chain_steps(status);
CREATE INDEX idx_chain_step_level ON approval_chain_steps(chain_id, approval_id, step_level);

-- 外键约束
ALTER TABLE approval_chain_steps
    ADD CONSTRAINT fk_chain_step_chain FOREIGN KEY (chain_id) REFERENCES approval_chains(id);

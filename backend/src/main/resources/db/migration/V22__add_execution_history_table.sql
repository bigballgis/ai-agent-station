CREATE TABLE execution_history (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    tenant_id BIGINT,
    user_id BIGINT,
    message TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_execution_history_agent_id ON execution_history(agent_id);
CREATE INDEX idx_execution_history_tenant_id ON execution_history(tenant_id);
CREATE INDEX idx_execution_history_timestamp ON execution_history(timestamp);

-- V16: 工作流引擎表
-- 工作流定义表
CREATE TABLE workflow_definitions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    nodes JSONB,
    edges JSONB,
    triggers JSONB,
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wf_def_tenant ON workflow_definitions(tenant_id);
CREATE INDEX idx_wf_def_status ON workflow_definitions(status);
CREATE INDEX idx_wf_def_name ON workflow_definitions(name);

-- 工作流实例表
CREATE TABLE workflow_instances (
    id BIGSERIAL PRIMARY KEY,
    workflow_definition_id BIGINT NOT NULL,
    workflow_name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    current_node_id VARCHAR(100),
    current_step INT,
    variables JSONB,
    input JSONB,
    output JSONB,
    started_by BIGINT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error VARCHAR(2000),
    tenant_id BIGINT NOT NULL
);

CREATE INDEX idx_wf_inst_def ON workflow_instances(workflow_definition_id);
CREATE INDEX idx_wf_inst_tenant ON workflow_instances(tenant_id);
CREATE INDEX idx_wf_inst_status ON workflow_instances(status);
CREATE INDEX idx_wf_inst_started ON workflow_instances(started_at);

-- 工作流节点执行日志表
CREATE TABLE workflow_node_logs (
    id BIGSERIAL PRIMARY KEY,
    instance_id BIGINT NOT NULL,
    node_id VARCHAR(100) NOT NULL,
    node_name VARCHAR(200),
    node_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    input JSONB,
    output JSONB,
    error VARCHAR(2000),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration BIGINT
);

CREATE INDEX idx_wf_node_log_inst ON workflow_node_logs(instance_id);
CREATE INDEX idx_wf_node_log_node ON workflow_node_logs(node_id);
CREATE INDEX idx_wf_node_log_status ON workflow_node_logs(status);

-- 外键约束
ALTER TABLE workflow_instances
    ADD CONSTRAINT fk_wf_inst_def FOREIGN KEY (workflow_definition_id) REFERENCES workflow_definitions(id);

ALTER TABLE workflow_node_logs
    ADD CONSTRAINT fk_wf_node_log_inst FOREIGN KEY (instance_id) REFERENCES workflow_instances(id);

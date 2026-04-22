-- V14: 数据变更审计日志表
CREATE TABLE data_change_logs (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    record_id VARCHAR(100) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    field_name VARCHAR(100),
    old_value TEXT,
    new_value TEXT,
    operator VARCHAR(100),
    operator_ip VARCHAR(50),
    operated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT
);

CREATE INDEX idx_dcl_table_record ON data_change_logs(table_name, record_id);
CREATE INDEX idx_dcl_operated_at ON data_change_logs(operated_at);
CREATE INDEX idx_dcl_tenant ON data_change_logs(tenant_id);

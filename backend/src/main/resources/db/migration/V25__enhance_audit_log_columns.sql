-- V25: Enhance audit log columns for financial compliance
-- Add resource_type and resource_id to system_logs for better audit tracking
ALTER TABLE system_logs ADD COLUMN IF NOT EXISTS resource_type VARCHAR(100);
ALTER TABLE system_logs ADD COLUMN IF NOT EXISTS resource_id VARCHAR(100);

-- Add operator_id and user_agent to data_change_logs for full audit trail
ALTER TABLE data_change_logs ADD COLUMN IF NOT EXISTS operator_id BIGINT;
ALTER TABLE data_change_logs ADD COLUMN IF NOT EXISTS user_agent VARCHAR(500);

-- Add index for resource-based queries
CREATE INDEX IF NOT EXISTS idx_system_logs_resource ON system_logs(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_dcl_operator_id ON data_change_logs(operator_id);

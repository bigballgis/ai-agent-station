-- Add version columns for optimistic locking
-- Only add columns that don't already exist
ALTER TABLE agents ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE alert_rules ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
-- workflow_definitions and deployment_histories already have a 'version' column for business use,
-- so we use 'optimistic_version' for JPA optimistic locking
ALTER TABLE workflow_definitions ADD COLUMN IF NOT EXISTS optimistic_version BIGINT DEFAULT 0;
ALTER TABLE deployment_histories ADD COLUMN IF NOT EXISTS optimistic_version BIGINT DEFAULT 0;

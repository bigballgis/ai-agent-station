-- 工作流定义添加 base_definition_id 列，用于关联同一工作流的不同版本
ALTER TABLE workflow_definitions ADD COLUMN IF NOT EXISTS base_definition_id BIGINT;

-- 为现有数据设置 base_definition_id 等于自身 id
UPDATE workflow_definitions SET base_definition_id = id WHERE base_definition_id IS NULL;

COMMENT ON COLUMN workflow_definitions.base_definition_id IS '基础定义ID，用于关联同一工作流的不同版本';

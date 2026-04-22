-- V13: 数据字典表
CREATE TABLE dict_types (
    id BIGSERIAL PRIMARY KEY,
    dict_name VARCHAR(100) NOT NULL,
    dict_type VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(10) DEFAULT 'active',
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE TABLE dict_items (
    id BIGSERIAL PRIMARY KEY,
    dict_type VARCHAR(100) NOT NULL,
    dict_label VARCHAR(200) NOT NULL,
    dict_value VARCHAR(200) NOT NULL,
    dict_sort INT DEFAULT 0,
    css_class VARCHAR(100),
    list_class VARCHAR(100),
    is_default VARCHAR(10) DEFAULT 'N',
    status VARCHAR(10) DEFAULT 'active',
    remark VARCHAR(500),
    tenant_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dict_items_type ON dict_items(dict_type);
CREATE INDEX idx_dict_items_type_status ON dict_items(dict_type, status);

-- Seed data: 常用字典类型
INSERT INTO dict_types (dict_name, dict_type, status, remark) VALUES
('Agent状态', 'agent_status', 'active', 'Agent运行状态'),
('Agent类型', 'agent_type', 'active', 'Agent类型分类'),
('测试用例状态', 'test_case_status', 'active', '测试用例状态'),
('测试执行状态', 'test_execution_status', 'active', '测试执行状态'),
('测试结果状态', 'test_result_status', 'active', '测试结果状态'),
('审批状态', 'approval_status', 'active', '审批流程状态'),
('部署状态', 'deployment_status', 'active', '部署状态'),
('告警级别', 'alert_severity', 'active', '告警严重程度'),
('告警状态', 'alert_status', 'active', '告警处理状态'),
('用户状态', 'user_status', 'active', '用户账户状态'),
('租户状态', 'tenant_status', 'active', '租户状态'),
('性别', 'sys_gender', 'active', '性别'),
('是否', 'sys_yes_no', 'active', '是否选项'),
('通知类型', 'notification_type', 'active', '通知类型'),
('LLM提供商', 'llm_provider', 'active', '大语言模型提供商');

-- Seed data: 字典项
INSERT INTO dict_items (dict_type, dict_label, dict_value, dict_sort) VALUES
('agent_status', '草稿', 'DRAFT', 1),
('agent_status', '已发布', 'PUBLISHED', 2),
('agent_status', '已下线', 'OFFLINE', 3),
('agent_status', '已归档', 'ARCHIVED', 4),
('test_case_status', '待审核', 'PENDING', 1),
('test_case_status', '已通过', 'APPROVED', 2),
('test_case_status', '已拒绝', 'REJECTED', 3),
('test_execution_status', '等待中', 'PENDING', 1),
('test_execution_status', '运行中', 'RUNNING', 2),
('test_execution_status', '已完成', 'COMPLETED', 3),
('test_execution_status', '已取消', 'CANCELLED', 4),
('test_execution_status', '失败', 'FAILED', 5),
('approval_status', '待审批', 'PENDING', 1),
('approval_status', '已通过', 'APPROVED', 2),
('approval_status', '已拒绝', 'REJECTED', 3),
('approval_status', '已撤回', 'WITHDRAWN', 4),
('deployment_status', '部署中', 'DEPLOYING', 1),
('deployment_status', '成功', 'SUCCESS', 2),
('deployment_status', '失败', 'FAILED', 3),
('deployment_status', '已回滚', 'ROLLED_BACK', 4),
('alert_severity', '严重', 'CRITICAL', 1),
('alert_severity', '高', 'HIGH', 2),
('alert_severity', '中', 'MEDIUM', 3),
('alert_severity', '低', 'LOW', 4),
('alert_severity', '信息', 'INFO', 5),
('user_status', '正常', 'active', 1),
('user_status', '禁用', 'inactive', 2),
('user_status', '锁定', 'locked', 3),
('tenant_status', '正常', 'active', 1),
('tenant_status', '禁用', 'inactive', 2),
('tenant_status', '过期', 'expired', 3),
('llm_provider', 'OpenAI', 'openai', 1),
('llm_provider', '通义千问', 'qwen', 2),
('llm_provider', 'Ollama(本地)', 'ollama', 3);

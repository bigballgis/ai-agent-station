-- V18: 权限矩阵表
CREATE TABLE permission_matrix (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(200),
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(200),
    permission_type VARCHAR(20) NOT NULL DEFAULT 'ALLOW',
    tenant_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_perm_matrix UNIQUE (role_id, resource_type, resource_id, permission_code)
);

CREATE INDEX idx_perm_matrix_role ON permission_matrix(role_id);
CREATE INDEX idx_perm_matrix_tenant ON permission_matrix(tenant_id);
CREATE INDEX idx_perm_matrix_resource ON permission_matrix(resource_type, resource_id);
CREATE INDEX idx_perm_matrix_code ON permission_matrix(permission_code);

-- 为管理员角色(admin)插入默认权限
-- 假设管理员角色ID为1，租户ID为1
INSERT INTO permission_matrix (role_id, resource_type, resource_id, permission_code, permission_name, permission_type, tenant_id) VALUES
-- Agent管理
(1, 'MENU', 'agent', 'agent:view', '查看Agent', 'ALLOW', 1),
(1, 'MENU', 'agent', 'agent:create', '创建Agent', 'ALLOW', 1),
(1, 'MENU', 'agent', 'agent:edit', '编辑Agent', 'ALLOW', 1),
(1, 'MENU', 'agent', 'agent:delete', '删除Agent', 'ALLOW', 1),
(1, 'MENU', 'agent', 'agent:publish', '发布Agent', 'ALLOW', 1),
-- 用户管理
(1, 'MENU', 'user', 'user:view', '查看用户', 'ALLOW', 1),
(1, 'MENU', 'user', 'user:create', '创建用户', 'ALLOW', 1),
(1, 'MENU', 'user', 'user:edit', '编辑用户', 'ALLOW', 1),
(1, 'MENU', 'user', 'user:delete', '删除用户', 'ALLOW', 1),
-- 角色管理
(1, 'MENU', 'role', 'role:view', '查看角色', 'ALLOW', 1),
(1, 'MENU', 'role', 'role:create', '创建角色', 'ALLOW', 1),
(1, 'MENU', 'role', 'role:edit', '编辑角色', 'ALLOW', 1),
(1, 'MENU', 'role', 'role:delete', '删除角色', 'ALLOW', 1),
-- 审批管理
(1, 'MENU', 'approval', 'approval:view', '查看审批', 'ALLOW', 1),
(1, 'MENU', 'approval', 'approval:approve', '审批操作', 'ALLOW', 1),
-- 工作流管理
(1, 'MENU', 'workflow', 'workflow:view', '查看工作流', 'ALLOW', 1),
(1, 'MENU', 'workflow', 'workflow:create', '创建工作流', 'ALLOW', 1),
(1, 'MENU', 'workflow', 'workflow:edit', '编辑工作流', 'ALLOW', 1),
(1, 'MENU', 'workflow', 'workflow:delete', '删除工作流', 'ALLOW', 1),
(1, 'MENU', 'workflow', 'workflow:publish', '发布工作流', 'ALLOW', 1),
(1, 'MENU', 'workflow', 'workflow:execute', '执行工作流', 'ALLOW', 1),
-- 租户管理
(1, 'MENU', 'tenant', 'tenant:view', '查看租户', 'ALLOW', 1),
(1, 'MENU', 'tenant', 'tenant:create', '创建租户', 'ALLOW', 1),
(1, 'MENU', 'tenant', 'tenant:edit', '编辑租户', 'ALLOW', 1),
-- 系统管理
(1, 'MENU', 'system', 'system:log', '查看日志', 'ALLOW', 1),
(1, 'MENU', 'system', 'system:config', '系统配置', 'ALLOW', 1),
(1, 'MENU', 'system', 'system:alert', '告警管理', 'ALLOW', 1),
(1, 'MENU', 'system', 'system:quota', '配额管理', 'ALLOW', 1),
-- API管理
(1, 'API', '*', 'api:access', 'API访问', 'ALLOW', 1),
-- 数据权限
(1, 'DATA', 'agent', 'data:agent:all', '所有Agent数据', 'ALLOW', 1),
(1, 'DATA', 'user', 'data:user:all', '所有用户数据', 'ALLOW', 1),
(1, 'DATA', 'log', 'data:log:all', '所有日志数据', 'ALLOW', 1);

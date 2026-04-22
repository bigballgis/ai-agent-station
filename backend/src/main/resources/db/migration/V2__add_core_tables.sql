-- 租户表
CREATE TABLE IF NOT EXISTS tenants (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  schema_name VARCHAR(100),
  is_active BOOLEAN NOT NULL DEFAULT true,
  api_key VARCHAR(100),
  api_secret VARCHAR(100),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_tenants_name UNIQUE (name),
  CONSTRAINT uk_tenants_api_key UNIQUE (api_key),
  CONSTRAINT uk_tenants_schema_name UNIQUE (schema_name)
);

-- 用户表
CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  username VARCHAR(100) NOT NULL,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(100),
  phone VARCHAR(50),
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_users_username UNIQUE (username)
);

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 权限表
CREATE TABLE IF NOT EXISTS permissions (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  resource_code VARCHAR(100),
  action_code VARCHAR(50),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS role_permissions (
  id BIGSERIAL PRIMARY KEY,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
);

-- 系统日志表
CREATE TABLE IF NOT EXISTS system_logs (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT,
  user_id BIGINT,
  username VARCHAR(100),
  module VARCHAR(100),
  operation VARCHAR(500),
  method VARCHAR(200),
  params TEXT,
  ip VARCHAR(50),
  user_agent VARCHAR(500),
  execution_time BIGINT,
  is_success BOOLEAN,
  error_msg TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_roles_tenant_id ON roles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_permissions_tenant_id ON permissions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions(permission_id);
CREATE INDEX IF NOT EXISTS idx_system_logs_tenant_id ON system_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_system_logs_user_id ON system_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_system_logs_created_at ON system_logs(created_at DESC);

-- 初始化默认租户
INSERT INTO tenants (name, description, schema_name, is_active) VALUES
('默认租户', '系统默认租户', 'public', true)
ON CONFLICT (name) DO NOTHING;

-- 初始化默认管理员用户 (密码: admin123, 已加密)
INSERT INTO users (tenant_id, username, password, email, is_active) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@aiagent.com', true)
ON CONFLICT (username) DO NOTHING;

-- 初始化默认角色
INSERT INTO roles (tenant_id, name, description) VALUES
(1, '超级管理员', '拥有系统所有权限'),
(1, '租户管理员', '管理租户下的所有资源'),
(1, '普通用户', '普通用户权限')
ON CONFLICT DO NOTHING;

-- 初始化默认权限
INSERT INTO permissions (tenant_id, name, description, resource_code, action_code) VALUES
(1, '用户管理', '用户管理权限', 'USER', 'MANAGE'),
(1, '角色管理', '角色管理权限', 'ROLE', 'MANAGE'),
(1, '租户管理', '租户管理权限', 'TENANT', 'MANAGE'),
(1, '日志查看', '日志查看权限', 'LOG', 'VIEW')
ON CONFLICT DO NOTHING;

-- 为管理员分配角色
INSERT INTO user_roles (user_id, role_id) 
SELECT 1, id FROM roles WHERE name = '超级管理员' AND tenant_id = 1
ON CONFLICT DO NOTHING;

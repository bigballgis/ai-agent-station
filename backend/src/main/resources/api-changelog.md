# API Changelog

> Base URL: `/api`
>
> 所有响应格式: `application/json`，统一结构: `{ "code": 200, "message": "success", "data": ... }`
>
> 认证方式: `Authorization: Bearer {token}` 或 `X-API-Key: {key}`
>
> 版本头: 请求头 `X-API-Version: 1|2`（默认 1），响应头 `X-API-Version` 返回当前版本

---

## 1. 认证管理 (Auth)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/auth/captcha` | 获取数学验证码 | 否 | - |
| POST | `/v1/auth/login` | 用户登录 | 否 | - |
| POST | `/v1/auth/register` | 用户注册 | 否 | - |
| POST | `/v1/auth/refresh` | 刷新Token | 否 | - |
| POST | `/v1/auth/logout` | 用户登出 | 是 | `auth:manage` |
| GET | `/v1/auth/userinfo` | 获取当前用户信息 | 是 | - |
| PUT | `/v1/auth/password` | 修改密码 | 是 | - |
| POST | `/v1/auth/reset-password` | 管理员重置密码 | 是 | `user:manage` |

### POST `/v1/auth/login` - 用户登录

**请求示例:**
```json
{
  "username": "admin",
  "password": "password123",
  "tenantId": 1,
  "captchaId": "abc123",
  "captchaAnswer": "15"
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

### POST `/v1/auth/register` - 用户注册

**请求示例:**
```json
{
  "username": "newuser",
  "password": "password123",
  "email": "user@example.com",
  "tenantId": 1
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "newuser",
    "email": "user@example.com"
  }
}
```

### GET `/v1/auth/captcha` - 获取验证码

**响应示例:**
```json
{
  "code": 200,
  "data": {
    "captchaId": "a1b2c3d4e5",
    "question": "12 + 8 = ?"
  }
}
```

### POST `/v1/auth/refresh` - 刷新Token

**请求示例:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### PUT `/v1/auth/password` - 修改密码

**请求示例:**
```json
{
  "oldPassword": "oldpass",
  "newPassword": "newpass123"
}
```

---

## 2. Agent 管理

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/agents` | 获取Agent列表（分页） | 是 | `agent:view` |
| GET | `/v1/agents/{id}` | 获取Agent详情 | 是 | `agent:view` |
| POST | `/v1/agents` | 创建Agent | 是 | `agent:create` |
| PUT | `/v1/agents/{id}` | 更新Agent | 是 | `agent:update` |
| DELETE | `/v1/agents/{id}` | 删除Agent | 是 | `agent:delete` |
| POST | `/v1/agents/{id}/copy` | 复制Agent | 是 | `agent:create` |
| GET | `/v1/agents/{id}/versions` | 获取Agent版本列表 | 是 | `agent:view` |
| GET | `/v1/agents/{id}/versions/{versionNumber}` | 获取指定版本详情 | 是 | `agent:view` |
| POST | `/v1/agents/{id}/versions/{versionNumber}/rollback` | 回滚到指定版本 | 是 | `agent:update` |
| GET | `/v1/agents/templates` | 获取模板列表 | 是 | - |
| POST | `/v1/agents/templates/{id}/use` | 使用模板创建Agent | 是 | `agent:create` |
| POST | `/v1/agents/templates/{id}/rate` | 为模板评分 | 是 | - |
| GET | `/v1/agents/export?id={id}` | 导出单个Agent为JSON | 是 | `agent:view` |
| GET | `/v1/agents/export-all` | 导出所有Agent为JSON数组 | 是 | `agent:view` |
| POST | `/v1/agents/import` | 从JSON导入Agent | 是 | `agent:create` |

### POST `/v1/agents` - 创建Agent

**请求示例:**
```json
{
  "name": "客服助手",
  "description": "智能客服Agent",
  "config": {
    "model": "gpt-4o",
    "systemPrompt": "你是一个专业的客服..."
  },
  "category": "customer-service",
  "isActive": true,
  "isTemplate": false,
  "type": "DRAFT"
}
```

**响应示例:**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "name": "客服助手",
    "description": "智能客服Agent",
    "status": "DRAFT",
    "category": "customer-service",
    "isActive": true,
    "createdAt": "2025-01-01 00:00:00"
  }
}
```

### POST `/v1/agents/{id}/copy` - 复制Agent

**请求示例:**
```json
{
  "newName": "客服助手-副本"
}
```

### POST `/v1/agents/templates/{id}/rate` - 模板评分

**请求参数:** `?rating=5`（1-5 星）

---

## 3. Agent API (调用与执行)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| POST | `/v1/agent/{agentId}/invoke` | 调用Agent | 是 | `agent:invoke` |
| GET | `/v1/agent/{agentId}/status` | 获取Agent状态 | 是 | `agent:invoke` |
| GET | `/v1/task/{taskId}` | 查询异步任务状态 | 是 | `agent:invoke` |

### POST `/v1/agent/{agentId}/invoke` - 调用Agent

**请求示例:**
```json
{
  "message": "你好，请帮我查询订单状态",
  "context": {},
  "async": false,
  "stream": false
}
```

**响应示例:**
```json
{
  "code": 200,
  "data": {
    "status": "SUCCESS",
    "result": "您的订单 #12345 当前状态为已发货...",
    "executionTime": 1250,
    "taskId": null
  }
}
```

### GET `/v1/task/{taskId}` - 查询异步任务

**响应示例:**
```json
{
  "code": 200,
  "data": {
    "taskId": "uuid-xxx",
    "status": "SUCCESS",
    "result": "...",
    "executionTime": 3200
  }
}
```

---

## 4. 工作流管理 (Workflow)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/workflows/definitions` | 分页查询工作流定义 | 是 | `workflow:view` |
| POST | `/v1/workflows/definitions` | 创建工作流定义 | 是 | `workflow:manage` |
| GET | `/v1/workflows/definitions/{id}` | 获取工作流定义详情 | 是 | `workflow:view` |
| PUT | `/v1/workflows/definitions/{id}` | 更新工作流定义 | 是 | `workflow:manage` |
| DELETE | `/v1/workflows/definitions/{id}` | 删除工作流定义 | 是 | `workflow:manage` |
| POST | `/v1/workflows/definitions/{id}/publish` | 发布工作流定义 | 是 | `workflow:manage` |
| POST | `/v1/workflows/definitions/{id}/new-version` | 创建新版本（草稿） | 是 | `workflow:manage` |
| POST | `/v1/workflows/definitions/{id}/rollback/{targetVersion}` | 回滚到指定版本 | 是 | `workflow:manage` |
| GET | `/v1/workflows/instances` | 分页查询工作流实例 | 是 | `workflow:view` |
| POST | `/v1/workflows/instances/start` | 启动工作流 | 是 | `workflow:manage` |
| GET | `/v1/workflows/instances/{id}` | 获取工作流实例详情 | 是 | `workflow:view` |
| GET | `/v1/workflows/instances/{id}/history` | 获取执行历史 | 是 | `workflow:view` |
| POST | `/v1/workflows/instances/{id}/cancel` | 取消工作流 | 是 | `workflow:manage` |
| POST | `/v1/workflows/instances/{id}/resume` | 恢复中断的工作流 | 是 | `workflow:manage` |
| POST | `/v1/workflows/instances/{instanceId}/nodes/{nodeId}/approve` | 审批通过 | 是 | `workflow:manage` |
| POST | `/v1/workflows/instances/{instanceId}/nodes/{nodeId}/reject` | 驳回节点 | 是 | `workflow:manage` |
| GET | `/v1/workflows/definitions/{id}/export` | 导出工作流定义为JSON | 是 | `workflow:view` |
| POST | `/v1/workflows/definitions/import` | 从JSON导入工作流定义 | 是 | `workflow:manage` |

### POST `/v1/workflows/definitions` - 创建工作流定义

**请求示例:**
```json
{
  "name": "订单审批流程",
  "description": "标准订单审批工作流",
  "nodes": [
    { "id": "node1", "type": "START", "name": "开始" },
    { "id": "node2", "type": "APPROVAL", "name": "经理审批" },
    { "id": "node3", "type": "END", "name": "结束" }
  ],
  "edges": [
    { "source": "node1", "target": "node2" },
    { "source": "node2", "target": "node3" }
  ],
  "triggers": []
}
```

### POST `/v1/workflows/instances/start` - 启动工作流

**请求示例:**
```json
{
  "definitionId": 1,
  "variables": {
    "orderId": "ORD-001",
    "amount": 5000
  }
}
```

### POST `/v1/workflows/instances/{id}/cancel` - 取消工作流

**请求示例:**
```json
{
  "reason": "业务需求变更"
}
```

### POST `/v1/workflows/instances/{instanceId}/nodes/{nodeId}/approve` - 审批

**请求示例:**
```json
{
  "comment": "同意"
}
```

---

## 5. API 接口管理 (API Gateway)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/api-interfaces` | 分页查询API接口列表 | 是 | `api:view` |
| GET | `/v1/api-interfaces/{id}` | 获取API接口详情 | 是 | `api:read` |
| GET | `/v1/api-interfaces/agent/{agentId}` | 根据Agent获取接口列表 | 是 | `api:read` |
| POST | `/v1/api-interfaces` | 创建API接口 | 是 | `api:manage` |
| POST | `/v1/api-interfaces/{id}/version` | 创建新版本 | 是 | `api:write` |
| GET | `/v1/api-interfaces/{id}/versions` | 获取所有版本 | 是 | `api:read` |
| PATCH | `/v1/api-interfaces/{id}/deprecate` | 废弃API接口 | 是 | `api:write` |
| PUT | `/v1/api-interfaces/{id}` | 更新API接口 | 是 | `api:write` |
| DELETE | `/v1/api-interfaces/{id}` | 删除API接口 | 是 | `api:delete` |
| PATCH | `/v1/api-interfaces/{id}/toggle` | 切换启用状态 | 是 | `api:write` |

### POST `/v1/api-interfaces` - 创建API接口

**请求头:** `X-Tenant-ID: {tenantId}`

**请求示例:**
```json
{
  "agentId": 1,
  "versionId": 1,
  "path": "/api/v1/agent/1/invoke",
  "method": "POST",
  "description": "客服Agent调用接口",
  "isActive": true,
  "apiVersion": "v1"
}
```

### PATCH `/v1/api-interfaces/{id}/deprecate` - 废弃接口

**请求示例:**
```json
{
  "message": "该接口已被 v2 版本替代"
}
```

### PATCH `/v1/api-interfaces/{id}/toggle` - 切换启用状态

**请求示例:**
```json
{
  "isActive": false
}
```

---

## 6. API 调用日志

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/api-call-logs` | 分页查询调用日志 | 是 | `api:view` |
| GET | `/v1/api-call-logs/stats` | 获取调用统计信息 | 是 | `api:view` |
| GET | `/v1/api-call-logs/{id}` | 获取日志详情 | 是 | `api:view` |
| GET | `/v1/api-call-logs/request/{requestId}` | 根据请求ID查询日志 | 是 | `api:view` |

### GET `/v1/api-call-logs` - 查询调用日志

**请求头:** `X-Tenant-ID: {tenantId}`

**查询参数:** `page`, `size`, `agentId`, `apiInterfaceId`, `status`, `clientIp`, `startTime`, `endTime`

### GET `/v1/api-call-logs/stats` - 调用统计

**查询参数:** `agentId`, `days`（默认 7，最大 365）

**响应示例:**
```json
{
  "code": 200,
  "data": {
    "totalCount": 1520,
    "avgExecutionTime": 850,
    "maxExecutionTime": 5200,
    "periodDays": 7,
    "statusDistribution": {
      "SUCCESS": 1400,
      "FAILED": 120
    }
  }
}
```

---

## 7. 工具管理 (Tools/MCP)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/tools` | 获取所有可用工具列表 | 是 | `tool:view` |
| GET | `/v1/tools/stats` | 获取工具统计信息 | 是 | `tool:read` |
| GET | `/v1/tools/{toolName}/source` | 查询工具来源 | 是 | `tool:read` |
| POST | `/v1/tools/refresh` | 刷新工具缓存 | 是 | `tool:manage` |
| GET | `/v1/tools/health` | 获取MCP工具健康状态 | 是 | - |
| POST | `/v1/tools/{toolId}/test-connection` | 测试工具连接 | 是 | `tool:manage` |

### GET `/v1/tools` - 工具列表

**响应示例:**
```json
{
  "code": 200,
  "data": {
    "total": 15,
    "functionTools": 10,
    "mcpTools": 5,
    "tools": [
      {
        "name": "web_search",
        "description": "搜索互联网信息",
        "group": "search",
        "source": "function",
        "parameterCount": 3
      }
    ]
  }
}
```

### GET `/v1/tools/health` - MCP工具健康状态

**响应示例:**
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "weather_tool",
      "healthStatus": "HEALTHY",
      "lastHealthCheck": "2025-01-01 12:00:00",
      "consecutiveFailures": 0,
      "avgResponseTime": 120,
      "active": true
    }
  ]
}
```

### POST `/v1/tools/{toolId}/test-connection` - 测试连接

**响应示例:**
```json
{
  "code": 200,
  "data": {
    "toolId": 1,
    "toolName": "weather_tool",
    "healthStatus": "HEALTHY",
    "lastHealthCheck": "2025-01-01 12:05:00",
    "consecutiveFailures": 0,
    "avgResponseTime": 95,
    "active": true
  }
}
```

---

## 8. 文件管理 (Files)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| POST | `/v1/files/upload` | 上传文件 | 是 | `file:upload` |
| GET | `/v1/files/download/{filePath}` | 下载文件 | 是 | `file:view` |
| GET | `/v1/files/list` | 获取文件列表 | 是 | `file:read` |
| DELETE | `/v1/files/{id}` | 删除文件 | 是 | `file:delete` |

### POST `/v1/files/upload` - 上传文件

**Content-Type:** `multipart/form-data`

**请求参数:** `file`（必需）, `subDir`（可选）

**响应示例:**
```json
{
  "code": 200,
  "data": {
    "fileName": "document.pdf",
    "filePath": "uploads/document.pdf",
    "fileSize": 102400,
    "contentType": "application/pdf",
    "uploadedAt": "2025-01-01 12:00:00"
  }
}
```

### GET `/v1/files/list` - 文件列表

**查询参数:** `subDir`（可选）

---

## 9. 用户管理 (Users)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/users` | 获取所有用户列表 | 是 | `user:read`, ADMIN |
| GET | `/v1/users/{id}` | 获取用户详情 | 是 | `user:read`, ADMIN |
| POST | `/v1/users` | 创建用户 | 是 | `user:write`, ADMIN |
| PUT | `/v1/users/{id}` | 更新用户信息 | 是 | `user:write`, ADMIN |
| DELETE | `/v1/users/{id}` | 删除用户 | 是 | `user:delete`, ADMIN |
| POST | `/v1/users/{id}/reset-password` | 重置用户密码 | 是 | `user:manage`, ADMIN |

### POST `/v1/users` - 创建用户

**请求示例:**
```json
{
  "username": "newuser",
  "password": "password123",
  "email": "user@example.com",
  "phone": "13800138000"
}
```

---

## 10. 角色管理 (Roles)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/roles` | 获取所有角色列表 | 是 | `role:read`, ADMIN |
| GET | `/v1/roles/{id}` | 获取角色详情 | 是 | `role:read`, ADMIN |
| POST | `/v1/roles` | 创建角色 | 是 | `role:write`, ADMIN |
| PUT | `/v1/roles/{id}` | 更新角色 | 是 | `role:write`, ADMIN |
| DELETE | `/v1/roles/{id}` | 删除角色 | 是 | `role:manage`, ADMIN |
| POST | `/v1/roles/users/{userId}/roles/{roleId}` | 分配角色给用户 | 是 | `role:manage`, ADMIN |
| DELETE | `/v1/roles/users/{userId}/roles/{roleId}` | 从用户移除角色 | 是 | `role:manage`, ADMIN |
| GET | `/v1/roles/user/{userId}` | 获取用户角色列表 | 是 | `role:read`, ADMIN |

---

## 11. 权限管理 (Permissions)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/permissions` | 获取所有权限列表 | 是 | `permission:read`, ADMIN |
| GET | `/v1/permissions/{id}` | 获取权限详情 | 是 | `permission:read`, ADMIN |
| POST | `/v1/permissions` | 创建权限 | 是 | `permission:manage`, ADMIN |
| PUT | `/v1/permissions/{id}` | 更新权限 | 是 | `permission:manage`, ADMIN |
| DELETE | `/v1/permissions/{id}` | 删除权限 | 是 | `permission:manage`, ADMIN |
| POST | `/v1/permissions/roles/{roleId}/permissions/{permissionId}` | 分配权限给角色 | 是 | `permission:manage`, ADMIN |
| DELETE | `/v1/permissions/roles/{roleId}/permissions/{permissionId}` | 从角色移除权限 | 是 | `permission:manage`, ADMIN |
| GET | `/v1/permissions/role/{roleId}` | 获取角色权限列表 | 是 | `permission:read`, ADMIN |

---

## 12. 租户管理 (Tenants)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/tenants` | 获取所有租户列表 | 是 | `tenant:read`, SUPER_ADMIN |
| GET | `/v1/tenants/{id}` | 获取租户详情 | 是 | `tenant:read`, SUPER_ADMIN |
| POST | `/v1/tenants` | 创建租户 | 是 | `tenant:write`, SUPER_ADMIN |
| PUT | `/v1/tenants/{id}` | 更新租户信息 | 是 | `tenant:write`, SUPER_ADMIN |
| DELETE | `/v1/tenants/{id}` | 删除租户 | 是 | `tenant:manage`, SUPER_ADMIN |
| POST | `/v1/tenants/{id}/regenerate-api-key` | 重新生成API密钥 | 是 | `tenant:manage`, SUPER_ADMIN |
| POST | `/v1/tenants/{id}/reactivate` | 重新激活租户 | 是 | `tenant:manage`, SUPER_ADMIN |

### POST `/v1/tenants` - 创建租户

**请求示例:**
```json
{
  "name": "示例企业",
  "code": "demo",
  "domain": "demo.aiagent.com",
  "contactEmail": "admin@demo.com",
  "maxAgents": 50,
  "maxUsers": 200
}
```

### POST `/v1/tenants/{id}/reactivate` - 重新激活租户

**响应示例:**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "name": "示例企业",
    "status": "ACTIVE",
    "reactivatedAt": "2025-01-01 12:00:00"
  }
}
```

---

## 13. 配额管理 (Quotas)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/quotas/tenant/{tenantId}` | 获取租户配额概要 | 是 | `quota:read`, ADMIN |
| GET | `/v1/quotas/tenant/{tenantId}/details` | 获取租户配额详情 | 是 | `quota:read`, ADMIN |
| PUT | `/v1/quotas/tenant/{tenantId}` | 更新租户配额限制 | 是 | `quota:manage`, ADMIN |

**请求头:** `X-Tenant-ID: {tenantId}`（必须与路径中的 tenantId 一致）

---

## 14. 缓存统计 (Cache Stats)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/cache-stats` | 获取缓存统计信息 | 是 | - |

### GET `/v1/cache-stats` - 缓存统计

**响应示例:**
```json
{
  "code": 200,
  "data": {
    "agents": {
      "cacheName": "agents",
      "hits": 1500,
      "misses": 300,
      "hitRate": 0.833,
      "size": 120
    },
    "users": {
      "cacheName": "users",
      "hits": 800,
      "misses": 100,
      "hitRate": 0.889,
      "size": 50
    }
  }
}
```

---

## 15. 告警管理 (Alerts)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/alerts/rules` | 获取告警规则列表 | 是 | `alert:view` |
| POST | `/v1/alerts/rules` | 创建告警规则 | 是 | `alert:manage` |
| PUT | `/v1/alerts/rules/{id}` | 更新告警规则 | 是 | `alert:manage` |
| DELETE | `/v1/alerts/rules/{id}` | 删除告警规则 | 是 | `alert:manage` |
| GET | `/v1/alerts/records` | 分页查询告警记录 | 是 | `alert:view` |
| GET | `/v1/alerts/records/active` | 获取活跃告警列表 | 是 | `alert:view` |
| GET | `/v1/alerts/stats` | 获取告警统计信息 | 是 | `alert:view` |
| POST | `/v1/alerts/{id}/resolve` | 解决告警记录 | 是 | `alert:manage` |

### POST `/v1/alerts/rules` - 创建告警规则

**请求示例:**
```json
{
  "name": "API错误率告警",
  "metric": "api_error_rate",
  "condition": "GREATER_THAN",
  "threshold": 10,
  "windowMinutes": 5,
  "severity": "HIGH",
  "webhookUrl": "https://hooks.example.com/alert",
  "enabled": true
}
```

---

## 16. 监控端点 (Actuator)

| 方法 | 路径 | 描述 | 认证 |
|------|------|------|------|
| GET | `/actuator/health` | 健康检查 | 否 |
| GET | `/actuator/info` | 应用信息 | 否 |
| GET | `/actuator/metrics` | Prometheus指标 | 是, ADMIN |
| GET | `/actuator/prometheus` | Prometheus格式指标 | 是, ADMIN |

---

## 17. 审批管理 (Approvals)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/approvals` | 分页查询审批列表 | 是 | `approval:view` |
| GET | `/v1/approvals/pending` | 获取待审批列表 | 是 | `approval:view` |
| GET | `/v1/approvals/{id}` | 获取审批详情 | 是 | `approval:view` |
| GET | `/v1/approvals/agent/{agentId}` | 根据Agent获取审批列表 | 是 | `approval:view` |
| POST | `/v1/approvals/submit` | 提交审批申请 | 是 | `approval:manage` |
| POST | `/v1/approvals/{id}/approve` | 审批通过 | 是 | `approval:manage` |
| POST | `/v1/approvals/{id}/reject` | 审批拒绝 | 是 | `approval:manage` |

### POST `/v1/approvals/submit` - 提交审批

**请求示例:**
```json
{
  "agentId": 1,
  "versionId": 1,
  "remark": "请审批发布"
}
```

---

## 18. 流式对话 (SSE Stream)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/stream/chat` | SSE流式对话(GET) | 是 | `agent:invoke` |
| POST | `/v1/stream/chat` | SSE流式对话(POST) | 是 | `agent:invoke` |
| GET | `/v1/stream/agent/{agentId}` | SSE流式Agent执行(GET) | 是 | `agent:invoke` |
| POST | `/v1/stream/agent/{agentId}` | SSE流式Agent执行(POST) | 是 | `agent:invoke` |

**SSE 事件格式:**
- `event: token` / `data: {"type":"token","content":"..."}`
- `event: done` / `data: {"type":"done","content":"最终输出"}`
- `event: error` / `data: {"type":"error","content":"错误信息"}`
- `event: node_start` / `data: {"nodeId":"llm-1","nodeType":"llm"}`
- `event: node_end` / `data: {"nodeId":"llm-1","status":"completed"}`

---

## 19. 会话管理 (Sessions)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/v1/sessions/online` | 获取在线会话列表 | 是 | `session:manage` |
| GET | `/v1/sessions/me` | 获取当前用户的会话列表 | 是 | `session:manage` |
| DELETE | `/v1/sessions/{sessionId}` | 踢出指定会话 | 是 | `session:manage` |
| DELETE | `/v1/sessions/user/{userId}` | 踢出用户所有设备 | 是 | `session:manage` |
| GET | `/v1/sessions/stats` | 获取会话统计信息 | 是 | `session:manage` |

---

## 20. 仪表盘 (Dashboard)

| 方法 | 路径 | 描述 | 认证 | 权限 |
|------|------|------|------|------|
| GET | `/api/dashboard/stats` | 获取仪表盘统计数据 | 是 | - |

### GET `/api/dashboard/stats` - 仪表盘统计

**响应示例:**
```json
{
  "code": 200,
  "data": {
    "agentCount": 25,
    "totalApiCalls": 15200,
    "activeUsers": 48,
    "systemHealth": "HEALTHY"
  }
}
```

---

## 版本历史

### v1.1.0 (Round 267-269)

#### Round 267: API 速率限制仪表盘集成
- 新增 `GET /v1/rate-limits/dashboard` 端点，提供速率限制仪表盘统计
- 仪表盘统计包含: 当前窗口总请求数、限流命中数(429)、Top 限流端点、各租户用量
- Dashboard `/api/dashboard/stats` 响应新增 `rateLimitStats` 字段
- 新增 `RateLimitDashboardService` 聚合 Redis 速率限制指标

#### Round 268: API 请求/响应日志增强
- 增强请求日志记录: POST/PUT 请求体（带敏感字段遮蔽）
- 增强响应日志记录: 响应体截断到 1000 字符（带敏感字段遮蔽）
- 新增客户端信息记录: User-Agent、Accept-Language
- 新增可配置日志级别: 特定端点使用 DEBUG 级别
- 新增 `api_call_audit_logs` 表用于审计日志持久化
- 新增 `ApiCallAuditLogService` 异步保存审计日志
- 敏感字段遮蔽: password, token, accessToken, secret, apiKey 等

#### Round 269: API 版本废弃 + 变更日志
- 新增 `@ApiDeprecation` 注解，支持 `sinceVersion`、`sunsetDate`、`replacement` 字段
- 新增 `ApiDeprecationAspect` 自动添加废弃响应头:
  - `X-API-Deprecated: true`
  - `Sunset: <date>`
  - `Deprecation: true`
  - `Warning: 299 - "message"`
  - `Link: <replacement>; rel="successor-version"`
- 新增 `GET /v1/api-changelog` 端点，返回 API 变更日志 JSON

---

### v1.2.0 (Round 277-279)

#### Round 277: README.md 全面增强
- README.md 重构为完整项目文档（含目录导航）
- 新增: 功能概览（核心能力、安全合规、多租户运维）
- 新增: 技术栈表格（前端/后端/基础设施）
- 新增: 系统架构 ASCII 图（整体架构 + 后端分层）
- 新增: 环境变量表格（必填/可选分类）
- 新增: API 概览表格（20 个模块、120+ 端点）
- 新增: 质量指标表格（截至 Round 270）
- 新增: 贡献指南快速概要

#### Round 278: 后端代码文档（package-info.java）
- 新增 `com.aiagent.controller/package-info.java` -- 33 个控制器概述
- 新增 `com.aiagent.service/package-info.java` -- 45+ 个服务概述（含 llm/、tool/ 子包）
- 新增 `com.aiagent.repository/package-info.java` -- 38 个 Repository 概述
- 新增 `com.aiagent.config/package-info.java` -- 28 个配置类概述
- 新增 `com.aiagent.security/package-info.java` -- 安全框架概述（含子包）

#### Round 279: API 文档增强
- OpenApiConfig: 新增 ExternalDocumentation 外部文档链接
- OpenApiConfig: 新增 5 个 Tag 描述（审批管理、流式对话、会话管理、速率限制、数据导出）
- OpenApiConfig: 增强 17 个现有 Tag 描述（补充技术细节）
- 新增 `docs/architecture.md` -- 系统架构文档（前端/后端/数据库/安全/可观测性/网络/部署）
- 新增 `docs/security.md` -- 安全措施文档（认证/授权/传输/输入/数据/API/审计/基础设施）
- 新增 `docs/deployment.md` -- 部署文档快速参考（链接到 DEPLOYMENT.md）

---

### v1.0.0
- 初始 API 发布
- 认证管理（登录、注册、Token管理、验证码、密码策略）
- Agent CRUD、版本管理、模板市场、导出/导入
- 工作流定义、实例、审批、导出/导入、版本管理
- API 接口管理、调用日志、版本控制、废弃机制
- 工具管理（MCP + Function Calling）、健康检查、连接测试
- 文件上传下载
- 用户、角色、权限管理
- 租户管理、配额管理、重新激活
- 缓存统计、告警管理
- API 版本头支持（X-API-Version）
- SSE 流式对话、Agent 流式执行
- 会话管理（在线会话、踢出、多设备）
- 审批管理（提交、审批、拒绝）
- 仪表盘统计

# 系统架构文档

> 版本: v1.0 | 最后更新: Round 279

---

## 1. 架构概览

AI Agent Station 采用前后端分离的微服务架构，基于 Spring Boot 3 + Vue 3 构建，
通过 Docker Compose 进行容器编排，实现一键部署。

### 1.1 整体架构图

```
                          +-------------------+
                          |     Nginx         |
                          | (Port 80/443)     |
                          | TLS + 反向代理     |
                          +--------+----------+
                                   |
                 +-----------------+------------------+
                 |                                    |
          +------+------+                     +-------+-------+
          |  Frontend   |                     |   Backend     |
          |  (Vue 3)    |                     | (Spring Boot) |
          |  Port 5173  |                     |  Port 8080    |
          +------+------+                     +-------+-------+
                 |                                    |
                 |              +---------------------+---------------------+
                 |              |           |                 |             |
                 |        +-----+----+ +----+-----+   +------+----+ +------+----+
                 |        |PostgreSQL| |  Redis   |   |Prometheus | | Grafana  |
                 |        | Port 5432| | Port 6379|   | Port 9090 | | Port 3000|
                 |        +----------+ +----------+   +-----------+ +----------+
                 |
          +------+------+     +------------------+
          |  LLM APIs  |     |  MCP Tools       |
          | OpenAI/Qwen |     |  External APIs   |
          | Ollama      |     |                  |
          +-------------+     +------------------+
```

---

## 2. 前端架构

### 2.1 技术选型

- **框架**: Vue 3.4 (Composition API + `<script setup>`)
- **语言**: TypeScript 5.x
- **构建**: Vite 5
- **UI**: Ant Design Vue 4 + Tailwind CSS
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **国际化**: vue-i18n (zh-CN / en-US)
- **图表**: Chart.js
- **测试**: Vitest (单元) + Playwright (E2E)

### 2.2 目录结构

```
frontend/src/
├── api/              # API 模块（按业务域划分）
├── components/       # 通用组件（ProTable、CodeEditor、EmptyState 等）
├── composables/      # 组合式函数（17 个）
│   ├── useLoading.ts
│   ├── usePagination.ts
│   ├── useConfirm.ts
│   ├── useWebSocket.ts
│   ├── useNetworkStatus.ts
│   └── ...
├── pages/            # 页面组件
├── store/            # Pinia 状态管理（7 个 store）
├── utils/            # 工具函数
│   ├── request.ts    # Axios 封装（拦截器、重试、Token 刷新）
│   ├── stream.ts     # SSE 流式请求
│   ├── authStorage.ts # Token 存储管理
│   ├── logger.ts     # 日志工具
│   └── formatUtils.ts # 国际化格式化
└── App.vue           # 根组件（ErrorBoundary 包裹）
```

### 2.3 关键设计

- **请求层**: Axios 封装统一处理 401/403/429/5xx，支持 GET 自动重试和 Token 自动刷新
- **状态管理**: 7 个标准化 Store，支持 $reset()、类型化 getters/actions、stale-while-revalidate 缓存
- **错误处理**: ErrorBoundary 组件捕获渲染错误，支持分类、上报、自动重试
- **实时通信**: WebSocket composable 支持指数退避重连和离线队列
- **性能优化**: defineAsyncComponent、路由懒加载、webpackChunkName、requestIdleCallback 预加载

---

## 3. 后端架构

### 3.1 分层架构

```
Controller 层 (REST API)
  ├── 接收 HTTP 请求
  ├── @Valid 参数校验
  ├── DTO <-> Entity 转换
  └── 返回 Result<T>
      │
Service 层 (业务逻辑)
  ├── @Transactional 事务管理
  ├── 业务规则校验
  ├── 数据组装与聚合
  └── 跨服务协调
      │
Repository 层 (数据访问)
  ├── Spring Data JPA
  ├── 租户安全查询
  ├── @EntityGraph N+1 优化
  └── @QueryHints 分页优化
      │
Entity 层 (数据模型)
  ├── JPA 实体
  ├── @SQLDelete 软删除
  └── 审计字段 (createdAt, updatedAt)
      │
PostgreSQL + Redis
```

### 3.2 包结构

| 包 | 职责 | 文件数 |
|---|------|--------|
| `controller/` | REST 控制器（33 个） | 33 |
| `service/` | 业务逻辑（含 llm/、tool/ 子包） | 50+ |
| `repository/` | 数据访问（38 个） | 38 |
| `entity/` | JPA 实体 | 40+ |
| `dto/` | 数据传输对象 | 50+ |
| `vo/` | 视图对象 | 16 |
| `config/` | Spring 配置 | 28 |
| `security/` | 安全框架 | 10+ |
| `aspect/` | AOP 切面 | 6 |
| `exception/` | 异常处理 | 10 |
| `annotation/` | 自定义注解 | 9 |
| `tenant/` | 多租户 | 6 |
| `engine/` | 执行引擎 | 10 |
| `mcp/` | MCP 工具网关 | 2 |
| `websocket/` | WebSocket | 3 |
| `gateway/` | API 网关 | 4 |
| `util/` | 工具类 | 6 |

### 3.3 关键设计

- **统一响应**: `Result<T>` 包装（code、message、data、timestamp、path）
- **异常体系**: 10 个自定义异常类，GlobalExceptionHandler 统一处理
- **事务管理**: 128 个 @Transactional(rollbackFor = Exception.class)
- **参数校验**: 27 个 @Valid，199 个 @Parameter，301 个 @Schema
- **审计追踪**: @Audited 注解 + AuditLogAspect（38 个方法）
- **敏感数据**: @Sensitive 注解 + SensitiveDataSerializer（11 个字段）

---

## 4. 数据库架构

### 4.1 PostgreSQL

- **版本**: 16 (pgvector 扩展)
- **迁移**: Flyway V1-V31
- **索引**: 20+ 复合性能索引
- **软删除**: 20 个表支持 @SQLDelete

### 4.2 核心表

| 表 | 说明 |
|---|------|
| users | 用户表 |
| roles | 角色表 |
| permissions | 权限表 |
| agents | Agent 定义表 |
| agent_versions | Agent 版本表 |
| workflow_definitions | 工作流定义表 |
| workflow_instances | 工作流实例表 |
| api_interfaces | API 接口表 |
| tenants | 租户表 |
| alert_rules | 告警规则表 |
| mcp_tools | MCP 工具表 |

### 4.3 Redis

- **用途**: 缓存、会话、分布式限流、ChatMemory
- **缓存区域**: 9 个（agents、tools、templates、dictTypes、tenantConfig、permissionList、dashboardStats、users、llmResponse）
- **限流**: Redis INCR + EXPIRE 滑动窗口

---

## 5. 安全架构

### 5.1 认证

- **JWT 双 Token**: Access Token (30min) + Refresh Token (7d)
- **API Key**: X-API-Key 头认证
- **密码策略**: BCrypt(12)、历史 5 次、复杂度校验
- **账户锁定**: 5 次失败 / 30 分钟

### 5.2 授权

- **RBAC**: 角色 -> 权限矩阵 -> 资源操作
- **注解**: @RequiresRole、@RequiresPermission
- **数据隔离**: 租户级数据隔离（15 个 Repository）

### 5.3 防护

- **安全头**: CSP (10 指令)、HSTS、COOP、COEP、X-Frame-Options、Permissions-Policy
- **输入校验**: SortFieldValidator (JPQL 注入)、路径遍历防护、Prompt 注入防御
- **数据加密**: AES-256-GCM (API Key 加密存储)
- **审计日志**: 全链路操作审计、数据变更记录

---

## 6. 可观测性

### 6.1 监控

- **Prometheus**: 4 个自定义指标（agent_invocations_total、execution_duration_seconds、active_users_gauge、api_response_time）
- **Grafana**: API 概览仪表盘
- **健康检查**: 6 组件（DB、Redis、磁盘、LLM、JWT、缓存）

### 6.2 日志

- **结构化**: JSON 格式（生产环境）
- **MDC**: traceId、tenantId、userId、responseTimeMs
- **审计**: api-requests.log 独立日志文件
- **保留**: 30 天滚动

### 6.3 告警

- **规则**: 可配置告警规则（阈值、窗口、严重级别）
- **通道**: Webhook / 邮件 / 站内信
- **重试**: 指数退避重试

---

## 7. 网络架构

### 7.1 Docker 网络

| 网络 | 成员 | 说明 |
|------|------|------|
| backend-network | Backend, PostgreSQL, Redis, Nginx | 后端服务通信 |
| frontend-network | Frontend, Backend, Nginx | 前端服务通信 |
| monitoring-network | Prometheus, Grafana | 监控网络（internal only） |

### 7.2 端口映射

| 服务 | 内部端口 | 外部暴露 |
|------|---------|---------|
| Nginx | 80/443 | 是 |
| Frontend | 5173 | 否（通过 Nginx） |
| Backend | 8080 | 否（通过 Nginx） |
| PostgreSQL | 5432 | 否 |
| Redis | 6379 | 否 |
| Prometheus | 9090 | 否 |
| Grafana | 3000 | 是 |

---

## 8. 部署架构

### 8.1 CI/CD

- **工具**: GitHub Actions
- **流水线**: 4 个 Job（前端构建、后端编译测试、安全扫描、部署）
- **策略**: develop 自动部署 staging，main 手动触发生产部署

### 8.2 资源限制

| 服务 | 内存限制 | CPU 限制 |
|------|---------|---------|
| PostgreSQL | 2 GB | 2.0 |
| Redis | 512 MB | 1.0 |
| Backend | 4 GB | 4.0 |
| Frontend | 256 MB | 0.5 |
| Nginx | 256 MB | 0.5 |
| Prometheus | 1 GB | 1.0 |
| Grafana | 512 MB | 0.5 |

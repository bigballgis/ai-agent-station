# AI Agent Station — 实施任务分解

> **关联规格**: [spec.md](./spec.md)
> **总预估工时**: 177 小时
> **分 3 个 Phase 执行，每个 Phase 内任务可并行

---

## Phase 1: 安全与稳定性（P0）— 34.5h

### Sprint 1.1: 敏感信息与认证安全（8h）

#### TASK-001: 移除所有硬编码敏感信息 [已完成]
- **优先级**: P0 | **预估**: 2h | **依赖**: 无
- **描述**: 将 application.yml、docker-compose.yml、k8s/*.yml 中所有密码、密钥改为环境变量引用
- **涉及文件**:
  - `backend/src/main/resources/application.yml`
  - `docker-compose.yml`
  - `k8s/backend.yml`, `k8s/postgres.yml`, `k8s/redis.yml`, `k8s/kustomization.yml`
- **实施步骤**:
  1. `application.yml`: `password: ${DB_PASSWORD}`, `jwt.secret: ${JWT_SECRET}`（移除默认值）
  2. `docker-compose.yml`: 所有密码改为 `${VAR}` 引用
  3. 创建 `.env.example` 模板文件
  4. K8s: 所有密码改为 `valueFrom.secretKeyRef`
- **验收标准**:
  - `grep -r "password.*postgres\|secret.*2024\|admin.*password" backend/src/main/resources/ docker/ k8s/` 返回空
  - `.env.example` 包含所有必需环境变量及说明
- **完成状态**: 已完成。`jwt.secret: ${JWT_SECRET}` 无默认值，`DB_PASSWORD`/`REDIS_PASSWORD`/`OPENAI_API_KEY`/`QWEN_API_KEY` 均通过环境变量注入。`.env.example` 已创建。

#### TASK-002: 完善 RBAC 权限控制
- **优先级**: P0 | **预估**: 4h | **依赖**: TASK-001
- **描述**: 修复 PermissionAspect 实现真实权限校验，为所有 Controller 添加权限注解
- **涉及文件**:
  - `backend/.../aspect/PermissionAspect.java`
  - `backend/.../security/SecurityConfig.java`
  - 所有 Controller（15 个）
- **实施步骤**:
  1. 修复 `PermissionAspect.checkPermission()` — 查询数据库验证用户是否拥有所需权限
  2. 定义权限常量: `SUPER_ADMIN`, `TENANT_ADMIN`, `USER`
  3. 为每个 Controller 方法添加 `@RequiresRole` 或 `@RequiresPermission` 注解
  4. 更新 `SecurityConfig` — 管理接口（/users, /roles, /tenants, /permissions）限制为 ADMIN
- **验收标准**:
  - 普通用户访问 `/users` 返回 403
  - 超级管理员可访问所有接口
  - 租户管理员可管理本租户资源

#### TASK-003: 输入校验全覆盖
- **优先级**: P0 | **预估**: 3h | **依赖**: TASK-001
- **描述**: 为所有 DTO 添加校验注解，Controller 方法添加 @Valid
- **涉及文件**:
  - 所有 DTO 类（新建 + 现有）
  - 所有 Controller
  - `GlobalExceptionHandler.java`
- **实施步骤**:
  1. 为 LoginRequest 添加 `@NotBlank`, `@Size(min=6)`
  2. 为所有新建 DTO 添加对应校验注解
  3. 所有 `@RequestBody` 参数添加 `@Valid`
  4. GlobalExceptionHandler 添加 `MethodArgumentNotValidException` 处理
- **验收标准**:
  - 发送 `{ "username": "", "password": "" }` 到 `/auth/login` 返回 400 + 校验错误详情

#### TASK-004: CORS 配置收紧 + 移除 Mock 登录
- **优先级**: P0 | **预估**: 1h | **依赖**: 无
- **描述**: 收紧 CORS 白名单，移除前端 Mock 登录回退逻辑
- **涉及文件**:
  - `backend/.../config/CorsConfig.java`
  - `src/pages/Login.vue`
- **实施步骤**:
  1. CorsConfig: 从 application.yml 读取允许的域名列表
  2. Login.vue: 移除 catch 块中的 mock token 逻辑
- **验收标准**:
  - 非白名单域名的请求被拒绝
  - 登录失败显示真实错误信息，不再自动登录

---

### Sprint 1.2: 架构基础（DTO 层 + TypeScript）（13h）

#### TASK-005: 引入 DTO 层 [已完成]
- **优先级**: P0 | **预估**: 6h | **依赖**: TASK-003
- **描述**: 为所有 Entity 创建 RequestDTO/ResponseDTO，使用 MapStruct 转换
- **涉及文件**:
  - 新建 `dto/` 包（约 20 个 DTO 类）
  - 新建 `mapper/` 包（MapStruct Mapper 接口）
  - 所有 Controller, 所有 Service
- **实施步骤**:
  1. 创建基础 DTO: `AgentRequestDTO`, `AgentResponseDTO`, `UserRequestDTO`, `UserResponseDTO` 等
  2. ResponseDTO 排除 password, apiKey, apiSecret 等敏感字段
  3. 创建 MapStruct Mapper 接口
  4. 修改 Controller 使用 DTO 接收参数和返回结果
  5. 修改 Service 层 Entity ↔ DTO 转换
- **验收标准**:
  - API 响应 JSON 中不包含 password, apiKey, apiSecret 字段
  - 所有 Controller 方法参数类型为 DTO 而非 Entity
- **完成状态**: 已完成。所有 27 个 Controller 均使用 DTO。`dto/` 包含 30+ DTO 类，`DTOConverter` 提供转换方法。

#### TASK-006: 消除 Map<String, Object> 参数
- **优先级**: P0 | **预估**: 2h | **依赖**: TASK-005
- **描述**: 将 AgentApprovalController 和 DeploymentController 的 Map 参数替换为 DTO
- **涉及文件**:
  - `AgentApprovalController.java`
  - `DeploymentController.java`
  - 新建 `ApprovalRequestDTO`, `DeployRequestDTO`
- **验收标准**:
  - 所有 Controller 方法参数类型安全，无 `Map<String, Object>`

#### TASK-007: TypeScript 严格模式
- **优先级**: P0 | **预估**: 4h | **依赖**: 无
- **描述**: 启用 TypeScript strict 模式，修复所有类型错误
- **涉及文件**:
  - `tsconfig.json`
  - 所有 `.ts` 和 `.vue` 文件
- **实施步骤**:
  1. `tsconfig.json`: `strict: true`, `noUnusedLocals: true`, `noUnusedParameters: true`
  2. 为 API 层定义类型接口（`AgentDTO`, `UserDTO`, `PageResult<T>` 等）
  3. 逐文件修复 `any` 类型
  4. 移除未使用的变量和参数
- **验收标准**:
  - `npx vue-tsc --noEmit` 无错误
  - `any` 类型使用 < 5 处（仅限确实需要的地方）

#### TASK-008: 前端公共工具提取 + API 统一
- **优先级**: P0 | **预估**: 2h | **依赖**: TASK-007
- **描述**: 提取重复的工具函数，统一 API 响应处理
- **涉及文件**:
  - 新建 `src/utils/format.ts`
  - 新建 `src/types/api.ts`
  - 新建 `src/styles/common.css`
  - `src/utils/request.ts`
  - 所有 `api/*.ts` 文件
- **实施步骤**:
  1. 创建 `format.ts`: formatDate, getStatusColor, getStatusText
  2. 创建 `api.ts`: ApiResponse<T>, PageResult<T>, PageQuery 接口
  3. 修改 Axios 响应拦截器，统一返回 `ApiResponse<T>`
  4. 修改所有 API 模块使用统一类型
  5. 统一 API 导入路径为 `@/utils/request`
- **验收标准**:
  - formatDate/getStatusColor 不在任何页面组件中重复定义
  - 所有 API 函数有明确的类型签名

---

### Sprint 1.3: 性能与数据库基础（13.5h）

#### TASK-009: 路由懒加载
- **优先级**: P0 | **预估**: 1h | **依赖**: 无
- **描述**: 所有页面组件改为动态导入
- **涉及文件**: `src/router/index.ts`
- **验收标准**: `dist/assets/` 中出现多个 chunk 文件

#### TASK-010: 异步任务持久化
- **优先级**: P0 | **预估**: 3h | **依赖**: 无
- **描述**: 将内存中的 ConcurrentHashMap 替换为 Redis 存储
- **涉及文件**: `AgentExecutionEngine.java`
- **实施步骤**:
  1. 使用 RedisTemplate 存储任务状态（Key: `task:{taskId}`, TTL: 1h）
  2. 修改 submitAsyncTask/getAsyncTaskStatus 方法
  3. 添加任务超时清理逻辑
- **验收标准**: 应用重启后仍可查询异步任务状态

#### TASK-011: 修复 SQL Migration 重复代码
- **优先级**: P0 | **预估**: 3h | **依赖**: 无
- **描述**: 重构 V6、V7 SQL，消除 create_tenant_schema 函数重复
- **涉及文件**:
  - `V6__add_test_tables.sql`
  - `V7__add_evolution_tables.sql`
- **实施步骤**:
  1. V6: 移除重复的 create_tenant_schema 函数，改为增量 ALTER TABLE
  2. V7: 同上
  3. 验证 Flyway checksum 变更需要新建迁移文件
- **验收标准**: V6/V7 SQL 文件行数减少 60%+

#### TASK-012: 合并租户表 + 修复外键
- **优先级**: P0 | **预估**: 2.5h | **依赖**: TASK-011
- **描述**: 统一租户表结构，修复 V6 外键引用错误
- **涉及文件**:
  - 新建 `V8__merge_tenant_tables.sql`
  - 修改 `V6__add_test_tables.sql`（如果 checksum 允许）
- **验收标准**: 数据库中只有一个 tenants 表，所有外键引用正确

#### TASK-013: 创建 Dockerfile
- **优先级**: P0 | **预估**: 2h | **依赖**: 无
- **描述**: 创建后端和前端的多阶段构建 Dockerfile
- **涉及文件**:
  - 新建 `backend/Dockerfile`
  - 新建 `Dockerfile.frontend`
- **实施步骤**:
  1. 后端: Maven build → JRE 17 runtime
  2. 前端: Node build → Nginx serve
  3. 添加 `.dockerignore` 文件
- **验收标准**: `docker-compose build` 成功构建所有镜像

#### TASK-014: K8s Secret 管理
- **优先级**: P0 | **预估**: 1h | **依赖**: TASK-001
- **描述**: 所有密码通过 K8s Secret 注入
- **涉及文件**: `k8s/*.yml`
- **验收标准**: K8s YAML 中无明文密码

---

## Phase 2: 架构优化与性能提升（P1）— 97h

### Sprint 2.1: 后端安全加固（6h）

#### TASK-015: API Key 传递方式 + TenantContextHolder 修复
- **优先级**: P1 | **预估**: 1h
- **涉及文件**: `JwtAuthenticationFilter.java`, `TenantInterceptor.java`, `WebMvcConfig.java`

#### TASK-016: X-Tenant-Id/X-User-Id 伪造修复 + Entity 脱敏
- **优先级**: P1 | **预估**: 2h
- **涉及文件**: `AgentApiController.java`, `User.java`

#### TASK-017: Nginx 安全头 + Redis 集群密码 + Actuator 权限
- **优先级**: P1 | **预估**: 1.5h
- **涉及文件**: `nginx.conf`, `redis-*.conf`, `application.yml`, `SecurityConfig.java`

#### TASK-018: 密码重置验证 + API Key 复杂度 + 请求体限制
- **优先级**: P1 | **预估**: 1.5h
- **涉及文件**: `UserController.java`, `TenantController.java`, `nginx.conf`, `application.yml`

---

### Sprint 2.2: 后端架构优化（18h）

#### TASK-019: API 路径版本统一 + RESTful 规范 [已完成]
- **优先级**: P1 | **预估**: 5h
- **涉及文件**: 所有 Controller, 前端 `request.ts`, 所有 `api/*.ts`
- **完成状态**: API 路径统一已完成。后端 context-path=/api，所有 Controller 使用 `/v1/*` 前缀，前端 baseURL 为 `http://localhost:8080/api`。RESTful 规范部分待完善。

#### TASK-020: 响应格式统一 + SecurityUtils 提取
- **优先级**: P1 | **预估**: 2h
- **涉及文件**: `AgentApiController.java`, 新建 `SecurityUtils.java`

#### TASK-021: 租户隔离逻辑抽象
- **优先级**: P1 | **预估**: 3h
- **涉及文件**: 新建 `TenantAwareRepository.java` 或 `TenantFilterAspect.java`

#### TASK-022: 消除魔法数字 + Lombok 统一
- **优先级**: P1 | **预估**: 5h
- **涉及文件**: 所有 Service, 所有 Entity, 新建枚举类

#### TASK-023: Gateway 冲突修复 + hibernate-types 修复
- **优先级**: P1 | **预估**: 1.5h
- **涉及文件**: `pom.xml`, 删除 `gateway/` 包

#### TASK-024: 死代码清理 + Result/UserPrincipal 去重
- **优先级**: P1 | **预估**: 1.5h
- **涉及文件**: `Result.java`, `UserPrincipal.java`, 删除 `Empty.vue` 等

---

### Sprint 2.3: 后端性能优化（8h）

#### TASK-025: 后端列表接口分页
- **优先级**: P1 | **预估**: 3h
- **涉及文件**: 所有 Controller, 所有 Service, `PageResult.java`

#### TASK-026: Redis 缓存策略
- **优先级**: P1 | **预估**: 3h
- **涉及文件**: `AgentService.java`, `PermissionService.java`, `UserService.java`

#### TASK-027: 数据库复合索引 + 连接池优化
- **优先级**: P1 | **预估**: 2h
- **涉及文件**: 新建 `V9__add_indexes.sql`, `application.yml`

---

### Sprint 2.4: 前端构建优化（4h）

#### TASK-028: Vite 分包策略 + 按需导入
- **优先级**: P1 | **预估**: 2h
- **涉及文件**: `vite.config.ts`, `package.json`

#### TASK-029: Nginx gzip + 构建预压缩
- **优先级**: P1 | **预估**: 1h
- **涉及文件**: `nginx.conf`, `vite.config.ts`

#### TASK-030: 前端 Bug 修复
- **优先级**: P1 | **预估**: 1h
- **涉及文件**: `ApprovalManagement.vue`, `TestExecutionList.vue`, `api/approval.ts`, `api/deployment.ts`

---

### Sprint 2.5: UI/UX 极致美化 — 布局与设计系统（14h）

#### TASK-031: 设计系统 Design Tokens
- **优先级**: P1 | **预估**: 2h
- **涉及文件**: `tailwind.config.js`
- **描述**: 定义完整的颜色、圆角、阴影、动画 Token

#### TASK-032: OA 办公布局重写
- **优先级**: P1 | **预估**: 8h
- **涉及文件**: `MainLayout.vue`（完全重写）, `App.vue`, `style.css`
- **描述**: 顶栏 + 可折叠侧边栏 + 主内容区 + 底部状态栏，苹果级设计

#### TASK-033: 暗色模式完整接入
- **优先级**: P1 | **预估**: 4h
- **涉及文件**: `App.vue`, `MainLayout.vue`, `useTheme.ts`, 所有页面组件

---

### Sprint 2.6: UI/UX 极致美化 — 页面重构（30h）

#### TASK-034: 路由过渡动画
- **优先级**: P1 | **预估**: 1h
- **涉及文件**: `App.vue`, `style.css`

#### TASK-035: 登录页面重设计
- **优先级**: P1 | **预估**: 4h
- **涉及文件**: `Login.vue`（完全重写）

#### TASK-036: Dashboard 数据可视化美化
- **优先级**: P1 | **预估**: 4h
- **涉及文件**: `Dashboard.vue`

#### TASK-037: 列表页面统一重构（AgentList + TestCaseList）
- **优先级**: P1 | **预估**: 6h
- **涉及文件**: `AgentList.vue`, `TestCaseList.vue`, 新建 `EmptyState.vue`

#### TASK-038: 列表页面统一重构（Approval + Deployment + TestExecution + TestResult）
- **优先级**: P1 | **预估**: 6h
- **涉及文件**: 4 个列表页面

#### TASK-039: 卡片组件 + 表单页面统一设计
- **优先级**: P1 | **预估**: 5h
- **涉及文件**: 新建 `AgentCard.vue`, `AgentEdit.vue`, `TestCaseEdit.vue`

#### TASK-040: 国际化全覆盖
- **优先级**: P1 | **预估**: 4h
- **涉及文件**: `zh-CN.ts`, `en-US.ts`, 所有页面组件

---

### Sprint 2.7: 部署基础设施（17h）

#### TASK-041: K8s Ingress 完善 + startupProbe
- **优先级**: P1 | **预估**: 1.5h
- **涉及文件**: `k8s/frontend.yml`, `k8s/backend.yml`

#### TASK-042: Prometheus 告警规则
- **优先级**: P1 | **预估**: 2h
- **涉及文件**: 新建 `docker/prometheus/alert_rules.yml`

#### TASK-043: Grafana Dashboard 配置
- **优先级**: P1 | **预估**: 3h
- **涉及文件**: 新建 `docker/grafana/dashboards/*.json`

#### TASK-044: Redis/PG Exporter + Docker 日志限制
- **优先级**: P1 | **预估**: 1.5h
- **涉及文件**: `docker-compose.yml`

#### TASK-045: 统一样式方案
- **优先级**: P1 | **预估**: 3h
- **涉及文件**: `tailwind.config.js`, 所有 `.vue` 文件

#### TASK-046: 响应式适配
- **优先级**: P1 | **预估**: 4h
- **涉及文件**: `MainLayout.vue`, 所有页面组件

#### TASK-047: 骨架屏 + 空状态组件
- **优先级**: P1 | **预估**: 2h
- **涉及文件**: 新建 `SkeletonCard.vue`, `SkeletonTable.vue`, `EmptyState.vue`

---

## Phase 3: 完善与优化（P2）— 45.5h

### Sprint 3.1: 高级 UI 组件（14h）

#### TASK-048: 全局搜索组件
- **优先级**: P2 | **预估**: 4h
- **涉及文件**: 新建 `GlobalSearch.vue`

#### TASK-049: 通知中心
- **优先级**: P2 | **预估**: 3h
- **涉及文件**: 新建 `NotificationCenter.vue`

#### TASK-050: AgentCanvas 拆分
- **优先级**: P2 | **预估**: 3h
- **涉及文件**: `AgentCanvas.vue` → 4 个子组件

#### TASK-051: 表单页面统一
- **优先级**: P2 | **预估**: 3h
- **涉及文件**: `AgentEdit.vue`, `TestCaseEdit.vue`

---

### Sprint 3.2: 真实 API 接入（9h）

#### TASK-052: 进化模块接入真实 API
- **优先级**: P2 | **预估**: 3h
- **涉及文件**: `evolution/*.vue`, 新建 API 函数

#### TASK-053: API 管理页面接入真实 API
- **优先级**: P2 | **预估**: 3h
- **涉及文件**: `ApiManagement.vue`, `ApiDocumentation.vue`

#### TASK-054: 日志脱敏 + 密码复杂度 + 请求体限制
- **优先级**: P2 | **预估**: 1.5h
- **涉及文件**: `RequestLoggingFilter.java`, `UserController.java`

#### TASK-055: PageResult 元数据 + JDBC URL 清理 + 连接池监控
- **优先级**: P2 | **预估**: 1.5h
- **涉及文件**: `PageResult.java`, `application.yml`

---

### Sprint 3.3: K8s 与 CI/CD（10h）

#### TASK-056: K8s PDB + 反亲和性 + 前端 HPA
- **优先级**: P2 | **预估**: 1.5h
- **涉及文件**: `k8s/*.yml`

#### TASK-057: mise.toml + index.html 修正
- **优先级**: P2 | **预估**: 0.5h
- **涉及文件**: `mise.toml`, `index.html`

#### TASK-058: CI/CD 流水线
- **优先级**: P2 | **预估**: 3h
- **涉及文件**: 新建 `.github/workflows/ci.yml`, `.github/workflows/deploy.yml`

#### TASK-059: Result/UserPrincipal 去重 + 死代码清理
- **优先级**: P2 | **预估**: 1h
- **涉及文件**: `Result.java`, `UserPrincipal.java`, 删除未使用文件

#### TASK-060: 集成测试补充
- **优先级**: P2 | **预估**: 4h
- **涉及文件**: `backend/src/test/`

---

### Sprint 3.4: 最终验收（12.5h）

#### TASK-061: 全量回归测试
- **优先级**: P2 | **预估**: 4h
- **描述**: 按照 checklist.md 逐项验证

#### TASK-062: 性能基准测试
- **优先级**: P2 | **预估**: 3h
- **描述**: 首屏加载时间、API 响应时间、缓存命中率

#### TASK-063: 安全扫描
- **优先级**: P2 | **预估**: 2h
- **描述**: OWASP ZAP 扫描、依赖漏洞检查

#### TASK-064: 文档更新
- **优先级**: P2 | **预估**: 2h
- **描述**: README、API 文档、部署指南

#### TASK-065: Code Review 与最终调整
- **优先级**: P2 | **预估**: 1.5h
- **描述**: 最终代码审查和细节调整

---

## 任务依赖关系图

```
Phase 1 (P0):
  TASK-001 ──→ TASK-002 ──→ TASK-005 ──→ TASK-006
  TASK-001 ──→ TASK-003 ──→ TASK-005
  TASK-004 (独立)
  TASK-007 (独立) ──→ TASK-008
  TASK-009 (独立)
  TASK-010 (独立)
  TASK-011 ──→ TASK-012
  TASK-013 (独立)
  TASK-001 ──→ TASK-014

Phase 2 (P1):
  TASK-001 ──→ TASK-015, TASK-016, TASK-017, TASK-018
  TASK-005 ──→ TASK-019, TASK-020
  TASK-021, TASK-022, TASK-023, TASK-024 (依赖 Phase 1 完成)
  TASK-025, TASK-026, TASK-027 (依赖 Phase 1 完成)
  TASK-028, TASK-029, TASK-030 (依赖 TASK-007, TASK-008)
  TASK-031 ──→ TASK-032 ──→ TASK-033
  TASK-032 ──→ TASK-034, TASK-035, TASK-036, TASK-037, TASK-038, TASK-039
  TASK-040 (依赖 TASK-008)
  TASK-041 ~ TASK-047 (依赖 Phase 1 完成)

Phase 3 (P2):
  依赖 Phase 2 全部完成
```

---

## 执行建议

1. **Phase 1 可 3 个 Sprint 并行执行**（安全组 / 架构组 / 性能组）
2. **Phase 2 中 UI/UX 任务量最大**（约 47h），建议分配 2 名前端开发者
3. **每个 Sprint 结束后进行 Code Review 和回归测试**
4. **Phase 3 可根据时间预算选择性执行**

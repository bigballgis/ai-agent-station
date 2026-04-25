# AegisNexus

> Financial-grade AI agent service gateway for secure enterprise automation in large banking environments.

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 17](https://img.shields.io/badge/Java-17-green.svg)](https://openjdk.org/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.2.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.4-42b883.svg)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-3178c6.svg)](https://www.typescriptlang.org/)

---

## 目录

- [功能概览](#功能概览)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [快速开始](#快速开始)
- [环境变量](#环境变量)
- [项目结构](#项目结构)
- [API 文档](#api-文档)
- [测试指南](#测试指南)
- [部署指南](#部署指南)
- [AI 开发入口](#ai-开发入口)
- [贡献指南](#贡献指南)
- [License](#license)

---

## 功能概览

### 核心能力

- **Agent 管理** -- 低代码 DAG 图编排、多 LLM 集成（OpenAI / Qwen / Ollama）、版本管理与回滚、模板市场与评分
- **工作流引擎** -- 可视化拖拽编排、DAG 图执行引擎、节点超时与并行策略、版本管理与回滚、执行恢复机制
- **API 网关** -- 统一接口管理、版本控制与废弃机制、调用日志与统计分析、速率限制仪表盘
- **测试体系** -- 用例管理、自动执行引擎、结果分析与报告；覆盖范围以当前 CI 与测试命令为准
- **审批流程** -- 多级审批链、工作流内嵌审批、审批历史追踪
- **部署管理** -- 一键部署、版本回滚、环境隔离

### 安全与合规

- **认证授权** -- JWT 双 Token 认证、RBAC 权限矩阵、API Key 认证
- **数据安全** -- AES-256-GCM 加密、BCrypt(12) 密码哈希、敏感字段脱敏
- **防护机制** -- CSP/HSTS 安全头、Prompt 注入防御、路径遍历防护、JPQL 注入防护
- **审计追踪** -- 全链路审计日志、数据变更记录、登录审计、密码历史追踪
- **账户安全** -- 登录失败锁定（5 次/30 分钟）、密码历史（5 次）、会话管理（最多 3 设备）

### 多租户与运维

- **多租户** -- 数据隔离、配额管理、租户生命周期（入驻/停用/重激活）
- **实时通信** -- WebSocket 事件推送（5 种事件类型）、离线队列、指数退避重连
- **可观测性** -- Prometheus 指标、Grafana 仪表盘、结构化 JSON 日志、6 组件健康检查
- **弹性容错** -- Spring Retry、熔断器、请求重试（5xx/429）、分布式限流

---

## 技术栈

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.4 | 核心框架（Composition API + `<script setup>`） |
| TypeScript | 5.x | 类型安全 |
| Vite | 5 | 构建工具 |
| Ant Design Vue | 4 | UI 组件库 |
| Pinia | - | 状态管理 |
| Vue Router | 4 | 路由管理 |
| vue-i18n | - | 国际化（zh-CN / en-US，翻译键数量以源码统计为准） |
| Chart.js | - | 数据可视化 |
| Tailwind CSS | - | 样式系统 + 暗黑模式 |
| Playwright | - | E2E 测试 |

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.2.4 | 应用框架 |
| Spring Security | - | 安全框架 |
| Spring Data JPA | - | ORM |
| PostgreSQL | 16 | 主数据库（pgvector） |
| Redis | 7 | 缓存 / 会话 / 限流 |
| LangChain4j | 0.36.2 | 多模型集成 |
| Flyway | - | 数据库迁移（V1-V31） |
| Resilience4j | - | 熔断 / 重试 / 限流 |
| SpringDoc OpenAPI | - | API 文档 |

### 基础设施

| 技术 | 用途 |
|------|------|
| Docker + Docker Compose | 容器编排（7 服务） |
| Nginx | 反向代理 / TLS 终止 |
| Prometheus + Grafana | 监控与告警 |
| GitHub Actions | CI/CD 流水线 |

---

## 系统架构

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

### 后端分层架构

```
Controller (REST API, DTO/VO)
    |
Service (业务逻辑, @Transactional)
    |
Repository (Spring Data JPA, 查询优化)
    |
Entity (JPA 实体, 软删除)
    |
PostgreSQL + Redis
```

### 关键设计

- **API 路径规范**: 后端 context-path `/api` + Controller 路由 `/v1/*` = 完整路径 `/api/v1/*`
- **统一响应格式**: `{ code, message, data, timestamp, path }`
- **分页**: 后端 0-based，前端 1-based（自动转换）
- **认证**: JWT Access Token + Refresh Token，支持 localStorage / sessionStorage 双存储
- **多租户**: 15 个 Repository 租户安全查询，配额强制执行

---

## 快速开始

### 环境要求

| 依赖 | 最低版本 | 说明 |
|------|---------|------|
| JDK | 17+ | OpenJDK 或 Oracle JDK |
| Node.js | 20+ | LTS 版本 |
| PostgreSQL | 16+ | 主数据库 |
| Redis | 7+ | 缓存服务 |
| Docker | 24+ | 可选，推荐用于快速部署 |

### Docker Compose 一键部署（推荐）

```bash
# 1. 克隆项目
git clone <repository-url>
cd aegisnexus

# 2. 复制环境变量模板并配置
cp .env.example .env
# 编辑 .env 填入 JWT_SECRET、DB_PASSWORD、REDIS_PASSWORD 等必填项

# 3. 启动所有服务
docker compose up -d

# 4. 查看服务状态
docker compose ps

# 5. 查看日志
docker compose logs -f backend
```

启动后访问：

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://localhost:5173 | Vue 3 SPA |
| 后端 API | http://localhost:8080/api | REST API |
| Swagger UI | http://localhost:8080/api/swagger-ui.html | API 文档 |
| Grafana | http://localhost:3000 | 监控仪表盘 |
| Prometheus | http://localhost:9090 | 指标查询 |

### 本地开发

#### 后端启动

```bash
cd backend
# 推荐先使用根目录 .env / docker compose 启动依赖
mvn compile "-Dmaven.test.skip=true"
mvn spring-boot:run
```

#### 前端启动

```bash
cd frontend
npm install
npm run dev
```

---

## 环境变量

### 必填变量

| 变量 | 说明 | 示例 |
|------|------|------|
| `DB_PASSWORD` | PostgreSQL 密码 | `your-strong-password` |
| `REDIS_PASSWORD` | Redis 密码 | `your-redis-password` |
| `JWT_SECRET` | JWT 签名密钥（至少 32 字符） | `base64-encoded-random-string` |
| `GRAFANA_ADMIN_PASSWORD` | Grafana 管理员密码 | `your-grafana-password` |

### 可选变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `JWT_EXPIRATION` | `3600000` | Access Token 有效期（毫秒） |
| `JWT_REFRESH_EXPIRATION` | `604800000` | Refresh Token 有效期（毫秒） |
| `LLM_DEFAULT_PROVIDER` | `openai` | 默认 LLM 提供商 |
| `OPENAI_API_KEY` | - | OpenAI API Key |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | OpenAI API 地址 |
| `OPENAI_DEFAULT_MODEL` | `gpt-4o` | 默认模型 |
| `QWEN_API_KEY` | - | 通义千问 API Key |
| `QWEN_BASE_URL` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | 千问 API 地址 |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama 地址 |
| `LANGCHAIN4J_MEMORY_STORE` | `in-memory` | ChatMemory 存储: `in-memory` / `redis` |
| `LANGCHAIN4J_TOOL_CALLING` | `true` | 是否启用 Tool Calling |
| `LOG_LEVEL` | `info` | 日志级别 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | CORS 允许的源 |

---

## 项目结构

```
aegisnexus/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/aiagent/
│   │   ├── controller/               # REST 控制器（33 个）
│   │   ├── service/                  # 业务服务层（含 llm/、tool/ 子包）
│   │   ├── engine/graph/             # DAG 图执行引擎
│   │   ├── entity/                   # JPA 实体
│   │   ├── repository/               # Spring Data JPA 仓库（38 个）
│   │   ├── dto/                      # 数据传输对象（请求/响应）
│   │   ├── vo/                       # 视图对象（16 个）
│   │   ├── config/                   # 配置类（28 个）
│   │   ├── security/                 # 安全框架（JWT、过滤器、密码策略）
│   │   ├── aspect/                   # AOP 切面（审计、限流、权限）
│   │   ├── tenant/                   # 多租户（数据源路由、拦截器）
│   │   ├── mcp/                      # MCP 工具网关
│   │   ├── websocket/                # WebSocket 通知
│   │   ├── gateway/                  # API 网关过滤器
│   │   ├── exception/                # 异常处理（10 个自定义异常）
│   │   ├── annotation/               # 自定义注解
│   │   └── util/                     # 工具类
│   └── src/main/resources/
│       ├── db/migration/             # Flyway 迁移脚本（V1-V31）
│       ├── api-changelog.md          # API 变更日志
│       ├── messages.properties       # i18n 消息（EN）
│       ├── messages_zh_CN.properties # i18n 消息（ZH）
│       └── application.yml           # 应用配置
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── pages/                    # 页面组件
│   │   ├── components/               # 通用组件（ProTable、CodeEditor 等）
│   │   ├── api/                      # API 模块
│   │   ├── store/                    # Pinia 状态管理
│   │   ├── composables/              # 组合式函数（17 个）
│   │   └── utils/                    # 工具函数
│   └── e2e/                          # Playwright E2E 测试
├── docker/                           # Docker 配置
│   ├── nginx/                        # Nginx 配置
│   ├── grafana/                      # Grafana 仪表盘
│   ├── prometheus/                   # Prometheus 配置
│   └── postgres/                     # PostgreSQL 初始化
├── docs/                             # 项目文档
├── docker-compose.yml                # 开发环境编排
├── docker-compose.prod.yml           # 生产环境编排
├── .env.example                      # 环境变量模板
├── .github/workflows/                # CI/CD 流水线
├── .cursor/skills/                   # 项目级 AI Skills
├── AGENTS.md                         # AI 编程代理统一入口
├── README.md                         # 本文件
├── DEPLOYMENT.md                     # 部署指南
├── CONTRIBUTING.md                   # 贡献指南
└── CLAUDE.md                         # Claude 简短入口（指向 AGENTS.md）
```

---

## API 文档

### 在线文档

启动后端服务后，访问 Swagger UI：

```
http://localhost:8080/api/swagger-ui.html
```

### API 概览

| 模块 | 路径前缀 | 端点数 | 说明 |
|------|---------|--------|------|
| 认证管理 | `/v1/auth` | 8 | 登录、注册、Token、验证码、密码 |
| Agent 管理 | `/v1/agents` | 14 | CRUD、版本、模板、导入导出 |
| Agent API | `/v1/agent` | 3 | 调用执行、状态查询 |
| 工作流管理 | `/v1/workflows` | 16 | 定义、实例、审批、导入导出 |
| API 接口 | `/v1/api-interfaces` | 10 | CRUD、版本、废弃 |
| API 调用日志 | `/v1/api-call-logs` | 4 | 日志查询、统计 |
| 工具管理 | `/v1/tools` | 6 | MCP + Function Calling |
| 文件管理 | `/v1/files` | 4 | 上传、下载、列表、删除 |
| 用户管理 | `/v1/users` | 6 | CRUD、密码重置 |
| 角色管理 | `/v1/roles` | 8 | CRUD、角色分配 |
| 权限管理 | `/v1/permissions` | 8 | CRUD、权限分配 |
| 租户管理 | `/v1/tenants` | 7 | CRUD、API 密钥、重激活 |
| 配额管理 | `/v1/quotas` | 3 | 配额查询与更新 |
| 缓存统计 | `/v1/cache-stats` | 1 | 缓存命中率统计 |
| 告警管理 | `/v1/alerts` | 8 | 规则 CRUD、记录查询 |
| 审批管理 | `/v1/approvals` | 7 | 提交、审批、拒绝 |
| 流式对话 | `/v1/stream` | 4 | SSE 流式对话与执行 |
| 会话管理 | `/v1/sessions` | 5 | 在线会话、踢出 |
| 仪表盘 | `/v1/dashboard` | 1 | 统计数据 |
| 监控端点 | `/actuator` | 4 | 健康检查、指标 |

### 认证方式

1. **JWT Bearer Token**: `Authorization: Bearer {token}`
2. **API Key**: `X-API-Key: {your-api-key}`

### API 版本

- 请求头 `X-API-Version: 1|2` 指定版本（默认: 1）
- 响应头 `X-API-Version` 返回当前版本

### 变更日志

详见 [api-changelog.md](backend/src/main/resources/api-changelog.md)

---

## 测试指南

### 后端测试

```bash
cd backend

# 主代码编译（当前最小基线）
mvn compile "-Dmaven.test.skip=true"

# 测试代码编译（用于发现测试与主代码是否同步）
mvn test-compile

# 运行全部测试
mvn test

# 运行单个测试类
mvn test -Dtest=AgentControllerTest

# 运行单个测试方法
mvn test -Dtest=AgentServiceTest#testCreateAgent
```

说明：若 `mvn test-compile` 失败，先按失败测试文件修复测试债，不要为了测试通过降低生产代码安全性。

### 前端测试

```bash
cd frontend

# 运行单元测试（Vitest）
npm run test:run

# 运行测试并生成覆盖率报告
npm run test:coverage

# E2E 测试（Playwright）
npx playwright test

# E2E 测试（指定浏览器）
npx playwright test --project=chromium
```

**测试覆盖范围**：
- 单元测试：组件、Composables、Store、工具函数（343 个用例）
- E2E 测试：登录、导航、仪表盘、Agent 列表（36 个用例）

### TypeScript 类型检查

```bash
cd frontend
npm run type-check
```

---

## 部署指南

详细的部署说明请参阅 [DEPLOYMENT.md](DEPLOYMENT.md)。

### 快速部署

```bash
# 生产环境部署
cp .env.production.example .env
docker compose -f docker-compose.prod.yml up -d --build

# Redis Cluster 模式（高可用）
docker compose --profile redis-cluster up -d
```

### CI/CD

- **develop 分支**: 推送后自动部署到 staging
- **main/master 分支**: 通过 GitHub Actions 手动触发部署
- **流水线**: 4 个 Job（前端构建、后端编译测试、安全扫描、部署）

---

## AI 开发入口

本仓库已按 AI 编程代理最佳实践整理入口：

- `AGENTS.md`：所有 AI 的统一规则入口。
- `CLAUDE.md`：Claude 专用短入口，避免长上下文污染。
- `docs/PLATFORM_V2_EXECUTION_PLAN.md`：v2 Core 重构总方案。
- `docs/AI_AGENT_READINESS.md`：AI 使用方式、Skills 与文档治理说明。
- `.cursor/skills/`：项目级 Skills，用于规划、实现、验证、架构审查和依赖升级。

推荐启动提示：

```text
请先读取 AGENTS.md，并使用 platform-v2-execution-plan 与 platform-v2-verification-gate。
执行 Phase 0 Task 1：恢复编译基线。一次只处理一个失败类别。
```

---

## 贡献指南

请参阅 [CONTRIBUTING.md](CONTRIBUTING.md) 了解详细的贡献流程。

### 快速概要

1. 从 `master` 创建 feature 分支
2. 遵循代码规范（后端阿里巴巴 Java 手册，前端 Vue 3 Composition API）
3. 按任务范围补充或修复测试
4. 提交 PR，至少 1 人 Code Review
5. CI 自动运行测试，通过后合并

### Git 提交规范

```
feat: 新功能
fix: 修复 Bug
docs: 文档更新
refactor: 代码重构
test: 测试相关
chore: 构建/工具变更
```

---

## 当前工程状态

当前主线是 `v2 Core` 重构准备阶段。可信状态以命令验证为准，不再以历史 Round 数字作为完成依据。

优先级：

1. 恢复后端主代码与测试代码编译基线。
2. 恢复前端类型检查与构建基线。
3. 稳定 Graph DSL v2 与运行时重构路径。
4. 以 Vue Flow POC 替换自研画布底座。
5. 统一 MCP / HTTP / Function Tool Plane。

---

## License

本项目基于 [MIT License](LICENSE) 开源。

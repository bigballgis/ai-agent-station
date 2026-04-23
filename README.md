# AI Agent Station

智能体低代码开发测试审批交付平台

## 技术栈

### 后端
- Java 17 + Spring Boot 3.2.4
- Spring Security + JWT 双Token认证
- Spring Data JPA + PostgreSQL 16
- Redis 7 (缓存/会话/限流)
- LangChain4j 0.36.2 (多模型集成)
- Resilience4j (熔断/重试/限流)
- Flyway 数据库迁移

### 前端
- Vue 3.4 + TypeScript + Vite 5
- Ant Design Vue 4
- Pinia 状态管理
- Chart.js 可视化
- Vue Router 4

### DevOps
- Docker + Docker Compose
- GitHub Actions CI/CD
- Prometheus + Grafana 监控
- Nginx 反向代理

## 快速开始

### 环境要求
- JDK 17+
- Node.js 20+
- PostgreSQL 16+
- Redis 7+

### 后端启动
```bash
cd backend
cp src/main/resources/application-example.yml src/main/resources/application.yml
# 编辑数据库和Redis连接配置
mvn clean package -DskipTests
java -jar target/ai-agent-platform-1.0.0.jar
```

### 前端启动
```bash
cd frontend
npm install
npm run dev
```

### Docker Compose 一键部署
```bash
docker compose up -d
```

## 项目结构

```
├── backend/
│   ├── src/main/java/com/aiagent/
│   │   ├── controller/     # 27个REST控制器
│   │   ├── service/        # 30+业务服务
│   │   ├── entity/         # 35+JPA实体
│   │   ├── repository/     # 35+数据仓库
│   │   ├── dto/            # 数据传输对象
│   │   ├── vo/             # 视图对象
│   │   ├── config/         # 配置类
│   │   ├── security/       # 安全框架
│   │   ├── aspect/         # AOP切面
│   │   ├── websocket/      # WebSocket
│   │   └── util/           # 工具类
│   └── src/main/resources/
│       ├── db/migration/   # 19个Flyway迁移脚本
│       └── application.yml
├── frontend/
│   ├── src/
│   │   ├── pages/          # 32个页面
│   │   ├── components/     # 30个通用组件
│   │   ├── api/            # 18个API模块
│   │   ├── store/          # 7个Pinia模块
│   │   ├── types/          # 6个类型定义
│   │   └── utils/          # 工具函数
│   └── vite.config.ts
├── docker-compose.yml
└── .github/workflows/      # CI/CD流水线
```

## 核心功能

- **Agent管理**: 低代码DAG图编排、多LLM集成、版本管理
- **测试体系**: 用例管理、自动执行、结果分析
- **审批流程**: 多级审批链、工作流引擎
- **部署管理**: 一键部署、回滚、环境隔离
- **权限控制**: RBAC + 权限矩阵 + 数据隔离
- **多租户**: 租户隔离、配额管理
- **安全**: JWT认证、Prompt注入防御、文件上传安全
- **监控**: 实时告警、操作审计、登录审计

## API 路径规范

### 前后端路径约定

| 层级 | 路径规则 | 示例 |
|------|----------|------|
| 后端 context-path | `/api` | Spring Boot `server.servlet.context-path=/api` |
| 后端 Controller 路由 | `/v1/*` | `@RequestMapping("/v1/auth")` |
| 完整后端路径 | `/api/v1/*` | `POST /api/v1/auth/login` |
| 前端 API 调用 | `/v1/*`（baseURL 已含 `/api`） | `request.ts` baseURL = `http://localhost:8080/api` |

所有 27 个 Controller 均已统一使用 `/v1/*` 前缀，前端通过 `request.ts` 的 `BASE_URL` 拼接完整路径。

### 认证接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 登录 | POST | `/api/v1/auth/login` | 返回 accessToken + refreshToken |
| 刷新 Token | POST | `/api/v1/auth/refresh` | 需传 refreshToken，返回新 accessToken |
| 登出 | POST | `/api/v1/auth/logout` | 使 refreshToken 失效，accessToken 加入黑名单 |
| 用户信息 | GET | `/api/v1/auth/userinfo | 返回当前登录用户信息 |

### Token 存储策略

前端通过 `authStorage.ts` 工具管理 Token，支持 **localStorage / sessionStorage 双存储**：

- `setToken(token, remember)` — `remember=true` 存 localStorage，否则存 sessionStorage
- `getToken()` — 优先读 localStorage，fallback 到 sessionStorage
- `clearAuth()` — 同时清除两个存储中的 token、refreshToken、userInfo
- Token 自动刷新：`request.ts` 响应拦截器在 401 时自动调用 `/v1/auth/refresh`

### SSE 流式接口

SSE 使用 **POST 方法**，通过 `stream.ts` 调用：

- `streamChat(params)` — `POST /api/v1/stream/chat`，流式对话
- `streamAgentExecution(agentId, params)` — `POST /api/v1/stream/agent/{agentId}`，流式 Agent 执行
- 底层使用 `fetch` + `ReadableStream` 接收 SSE 事件流

### DTO 改造

所有 27 个 Controller 均已完成 DTO 化，Controller 层接收 RequestDTO、返回 `Result<ResponseDTO>`，不再直接暴露 Entity。敏感字段（password、apiKey、apiSecret）已通过 DTO 隔离。

### 环境变量与密钥策略

所有敏感配置通过环境变量注入，无硬编码默认值：

| 环境变量 | 用途 | 配置项 |
|----------|------|--------|
| `JWT_SECRET` | JWT 签名密钥（必填，无默认值） | `jwt.secret` |
| `JWT_EXPIRATION` | Access Token 过期时间 | `jwt.expiration` |
| `JWT_REFRESH_EXPIRATION` | Refresh Token 过期时间 | `jwt.refresh-expiration` |
| `DB_PASSWORD` | PostgreSQL 密码 | `spring.datasource.password` |
| `REDIS_PASSWORD` | Redis 密码 | `spring.data.redis.password` |
| `OPENAI_API_KEY` | OpenAI API Key | `ai-agent.llm.openai.api-key` |
| `QWEN_API_KEY` | 通义千问 API Key | `ai-agent.llm.qwen.api-key` |

## API文档

启动后端后访问: http://localhost:8080/api/swagger-ui.html

## License

MIT

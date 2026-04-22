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

## API文档

启动后端后访问: http://localhost:8080/api/swagger-ui.html

## License

MIT

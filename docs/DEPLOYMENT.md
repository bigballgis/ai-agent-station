# AI Agent Platform - 云原生架构部署指南

## 项目概述

本项目是一个企业级的AI Agent平台，包含完整的云原生基础设施、数据库设计、监控系统和部署配置。

## 技术栈

- **数据库**: PostgreSQL 16 + pgvector (支持向量存储和检索)
- **缓存**: Redis 7 (支持单机和集群模式)
- **监控**: Prometheus + Grafana
- **后端**: Spring Boot 3.x + Java 17
- **前端**: Vue 3 + TypeScript + Vite
- **部署**: Docker Compose + Kubernetes
- **迁移**: Flyway

## 快速开始

### 使用Docker Compose部署

1. 克隆项目并进入目录
```bash
cd /workspace
```

2. 启动所有服务
```bash
# 启动核心服务 (默认)
docker-compose up -d

# 如需使用Redis Cluster，使用profile
docker-compose --profile redis-cluster up -d
```

3. 查看服务状态
```bash
docker-compose ps
```

4. 访问应用
- 前端: http://localhost:5173
- 后端API: http://localhost:8080/api
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- PostgreSQL: localhost:5432 (postgres/postgres)

### 停止服务
```bash
docker-compose down

# 停止同时删除数据卷 (谨慎使用)
docker-compose down -v
```

## 数据库架构设计

### 核心表结构

#### 1. 系统级表 (public schema)
- `tenant`: 租户信息表
- `system_config`: 系统配置表

#### 2. 租户级表 (每个租户独立schema，如t_default)
- `user`: 用户表
- `role`: 角色表
- `permission`: 权限表
- `user_role`: 用户角色关联表
- `role_permission`: 角色权限关联表
- `agent`: AI Agent表 (包含JSONB配置字段)
- `agent_version`: Agent版本表
- `agent_approval`: Agent审批表
- `api_interface`: API接口表
- `api_call_log`: API调用日志表
- `mcp_server`: MCP服务器表
- `mcp_tool`: MCP工具表
- `model_config`: 模型配置表
- `memory`: Agent记忆表 (包含JSONB和向量字段)
- `system_log`: 系统日志表

### 三级租户隔离机制

1. **公共Schema (public)**: 存放租户元数据和系统配置
2. **租户专属Schema**: 每个租户拥有独立的数据库Schema，如 `t_{tenant_id}`
3. **数据隔离**: 所有业务数据存放在租户专属Schema中，确保数据安全

### 初始化数据

系统初始化时自动创建:
- 默认租户 (tenant_code: default)
- 基础角色 (超级管理员、租户管理员、Agent设计者、审批者、观察者)
- 权限配置
- 系统配置参数

## Redis配置

### 单机模式 (默认)
```yaml
spring:
  data:
    redis:
      host: redis
      port: 6379
```

### 集群模式
```bash
# 启动集群模式
docker-compose --profile redis-cluster up -d
```

配置 `application.yml`:
```yaml
spring:
  data:
    redis:
      cluster:
        max-redirects: 3
        nodes:
          - redis-1:7001
          - redis-2:7002
          - redis-3:7003
          - redis-4:7004
          - redis-5:7005
          - redis-6:7006
```

## Kubernetes部署

### 前置条件
- Kubernetes集群 (版本 1.20+)
- kubectl配置
- Ingress Controller (可选但推荐)

### 部署步骤

1. 创建命名空间和基础资源
```bash
kubectl apply -f k8s/namespace.yml
```

2. 部署数据库和缓存
```bash
kubectl apply -f k8s/postgres.yml
kubectl apply -f k8s/redis.yml
```

3. 部署应用服务
```bash
kubectl apply -f k8s/backend.yml
kubectl apply -f k8s/frontend.yml
```

### 使用Kustomize一键部署
```bash
# 编辑secret信息
kubectl create secret generic ai-agent-secrets \
  --from-literal=openai-api-key=your-key \
  --from-literal=qwen-api-key=your-key \
  -n ai-agent-platform

# 部署所有服务
kubectl apply -k k8s/
```

### 查看部署状态
```bash
kubectl get all -n ai-agent-platform
kubectl get pods -n ai-agent-platform -w
```

### 访问应用
```bash
# 查看Ingress配置
kubectl get ingress -n ai-agent-platform

# 配置本地hosts文件添加:
# <集群IP> ai-agent.local
```

## 监控与可观测性

### Prometheus指标采集
Prometheus自动采集以下指标:
- JVM虚拟机指标
- 系统资源使用
- API请求统计
- 数据库连接池状态
- 缓存命中率

### Grafana监控仪表板
访问 Grafana: http://localhost:3000 (admin/admin)

预配置的数据源:
- Prometheus: http://prometheus:9090

## 环境变量配置

### 后端环境变量
| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| SPRING_DATASOURCE_URL | PostgreSQL连接地址 | jdbc:postgresql://localhost:5432/ai_agent_platform |
| SPRING_DATASOURCE_USERNAME | 数据库用户名 | postgres |
| SPRING_DATASOURCE_PASSWORD | 数据库密码 | postgres |
| SPRING_DATA_REDIS_HOST | Redis主机 | redis |
| SPRING_DATA_REDIS_PORT | Redis端口 | 6379 |
| OPENAI_API_KEY | OpenAI API Key | 空 |
| QWEN_API_KEY | 通义千问 API Key | 空 |

## 数据备份与恢复

### PostgreSQL备份
```bash
# 备份
docker exec -t ai-agent-postgres pg_dumpall -c -U postgres > backup.sql

# 恢复
cat backup.sql | docker exec -i ai-agent-postgres psql -U postgres -d ai_agent_platform
```

### Redis备份
```bash
# Redis数据持久化存储在redis_data卷中
# 如需备份，复制dump.rdb文件
```

## 故障排查

### 查看服务日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f postgres
```

### 数据库迁移问题
```bash
# 进入后端容器手动执行Flyway
docker exec -it ai-agent-backend sh
# 检查迁移状态
./mvnw flyway:info
```

### 健康检查
所有服务都配置了健康检查端点:
- 后端: `/api/actuator/health`
- PostgreSQL: 使用 `pg_isready`
- Redis: 使用 `ping`

## 性能优化建议

1. **数据库优化**:
   - 为频繁查询的字段创建索引
   - 定期 vacuum analyze
   - 配置合理的连接池大小

2. **缓存策略**:
   - 使用Redis缓存热点数据
   - 配置合适的TTL
   - 考虑使用Redis Cluster提高可用性

3. **应用扩展**:
   - 使用Kubernetes HPA自动扩缩容
   - 配置合理的资源限制
   - 实现API限流

## 安全性

1. 修改默认密码
2. 使用HTTPS
3. 配置网络策略 (Kubernetes)
4. 定期更新依赖包
5. 配置日志审计

## 贡献指南

请参考项目主README文档。

## 许可证

本项目使用MIT许可证。

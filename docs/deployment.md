# 部署文档

> 完整的部署指南请参阅项目根目录的 [DEPLOYMENT.md](../DEPLOYMENT.md)。

---

## 快速参考

### 环境要求

- Docker 24+ 和 Docker Compose v2+
- Git
- 容器镜像仓库访问权限

### 必填密钥

| 密钥 | 环境变量 | 说明 |
|------|---------|------|
| 数据库密码 | `DB_PASSWORD` | PostgreSQL 密码 |
| Redis 密码 | `REDIS_PASSWORD` | Redis 密码 |
| JWT 密钥 | `JWT_SECRET` | JWT 签名密钥（至少 32 字符） |
| Grafana 密码 | `GRAFANA_ADMIN_PASSWORD` | Grafana 管理员密码 |

### 一键部署

```bash
# 开发环境
cp .env.example .env
docker compose up -d

# 生产环境
cp .env.production.example .env
docker compose -f docker-compose.prod.yml up -d --build
```

### 服务验证

```bash
# 健康检查
curl http://localhost:8080/api/actuator/health

# 服务状态
docker compose ps

# 查看日志
docker compose logs -f backend
```

### 端口说明

| 服务 | 端口 | 说明 |
|------|------|------|
| Nginx | 80/443 | 反向代理入口 |
| Frontend | 5173 | Vue 3 SPA（通过 Nginx 代理） |
| Backend | 8080 | Spring Boot API（通过 Nginx 代理） |
| Grafana | 3000 | 监控仪表盘 |
| Prometheus | 9090 | 指标查询 |

---

## 详细文档

完整的部署指南（包括网络隔离、资源限制、CI/CD、回滚、监控、故障排除等）请参阅:

- [DEPLOYMENT.md](../DEPLOYMENT.md) -- 完整部署指南
- [architecture.md](architecture.md) -- 系统架构文档
- [security.md](security.md) -- 安全措施文档

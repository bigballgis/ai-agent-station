# AI Agent Platform - Deployment Guide

## Prerequisites

- Docker and Docker Compose v2+
- Git
- Access to the container registry (GHCR or private registry)
- Required secrets: `DB_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, `GRAFANA_ADMIN_PASSWORD`

## Quick Start

### 1. Clone and Configure

```bash
git clone <repository-url>
cd ai-agent-platform

# Create production environment file
cp .env.production.example .env.production
# Edit .env.production with your production values
```

### 2. Environment Variables

Create `.env.production` with the following required variables:

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_PASSWORD` | PostgreSQL password | (strong random password) |
| `REDIS_PASSWORD` | Redis password | (strong random password) |
| `JWT_SECRET` | JWT signing key (min 32 chars) | (base64 encoded random string) |
| `GRAFANA_ADMIN_PASSWORD` | Grafana admin password | (strong random password) |
| `OPENAI_API_KEY` | OpenAI API key (optional) | `sk-...` |
| `QWEN_API_KEY` | Qwen API key (optional) | `sk-...` |

### 3. Start Services

```bash
# Production deployment
docker compose -f docker-compose.prod.yml --env-file .env.production up -d

# Check service status
docker compose -f docker-compose.prod.yml ps

# View logs
docker compose -f docker-compose.prod.yml logs -f
```

### 4. Verify Health

```bash
# Backend health check
curl http://localhost:8080/api/actuator/health

# Detailed health check
curl http://localhost:8080/api/actuator/health/detail

# Prometheus metrics
curl http://localhost:9090/api/v1/targets

# Grafana dashboard
# Open http://localhost:3000 in browser (admin / GRAFANA_ADMIN_PASSWORD)
```

## Architecture

```
                    +-------+
                    | Nginx |  (Port 80/443)
                    +---+---+
                        |
              +---------+---------+
              |                   |
        +-----+-----+     +------+------+
        | Frontend  |     |  Backend    |
        | (Vue.js)  |     | (Spring)    |
        +-----------+     +------+------+  (Port 8080)
                                |
                    +-----------+-----------+
                    |           |           |
              +-----+---+ +----+----+ +----+-----+
              |PostgreSQL| | Redis   | |Prometheus|
              | (Port    | | (Port   | | (Port    |
              |  5432)   | |  6379)  | |  9090)   |
              +----------+ +---------+ +----------+
                                                |
                                          +-----+-----+
                                          |  Grafana  |
                                          | (Port     |
                                          |  3000)    |
                                          +-----------+
```

## Network Isolation

- **backend-network**: Backend, PostgreSQL, Redis, Nginx
- **frontend-network**: Frontend, Backend, Nginx
- **monitoring-network**: Prometheus, Grafana (internal only, no external access)

## Resource Limits (Production)

| Service | Memory Limit | CPU Limit | Memory Reserved |
|---------|-------------|-----------|-----------------|
| PostgreSQL | 2 GB | 2.0 | 512 MB |
| Redis | 512 MB | 1.0 | 128 MB |
| Backend | 4 GB | 4.0 | 1 GB |
| Frontend | 256 MB | 0.5 | 64 MB |
| Nginx | 256 MB | 0.5 | 64 MB |
| Prometheus | 1 GB | 1.0 | 256 MB |
| Grafana | 512 MB | 0.5 | 128 MB |

## CI/CD Pipeline

### Automatic Deployment

- **develop branch**: Auto-deploy to staging on push
- **main/master branch**: Manual deployment via GitHub Actions workflow_dispatch

### Manual Deployment

```bash
# Deploy to staging
docker compose --env-file .env.staging up -d

# Deploy to production
docker compose -f docker-compose.prod.yml --env-file .env.production up -d

# Rolling update (zero-downtime)
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d --remove-orphans
```

### Rollback

```bash
# View previous images
docker images | grep ai-agent

# Rollback to specific version
IMAGE_TAG=<previous-tag> docker compose -f docker-compose.prod.yml up -d
```

## Monitoring

- **Health Endpoint**: `GET /api/actuator/health`
- **Detailed Health**: `GET /api/actuator/health/detail`
- **Prometheus Metrics**: `GET /api/actuator/prometheus`
- **Grafana**: `http://localhost:3000`

### Key Metrics

- `http_server_requests_seconds` - API response time
- `agent_invocations_total` - Agent invocation count
- `agent_execution_duration_seconds` - Agent execution time
- `active_users_gauge` - Active user count
- JVM metrics (memory, threads, GC)
- HikariCP connection pool metrics

## Troubleshooting

```bash
# Check all service health
docker compose -f docker-compose.prod.yml ps

# View backend logs
docker compose -f docker-compose.prod.yml logs backend --tail=100 -f

# Check database connectivity
docker compose -f docker-compose.prod.yml exec postgres pg_isready -U postgres

# Check Redis connectivity
docker compose -f docker-compose.prod.yml exec redis redis-cli -a $REDIS_PASSWORD ping

# Restart a single service
docker compose -f docker-compose.prod.yml restart backend

# Full reset (WARNING: deletes data)
docker compose -f docker-compose.prod.yml down -v
```

## Security Notes

- All ports bind to `127.0.0.1` by default (not exposed externally)
- Nginx handles TLS termination and external access
- Monitoring network is isolated (`internal: true`)
- Secrets are passed via environment variables, never in images
- Enable TLS in Nginx by placing certificates in `docker/nginx/ssl/`

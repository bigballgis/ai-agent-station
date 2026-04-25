# Deployment Quick Reference

> See the root [`DEPLOYMENT.md`](../DEPLOYMENT.md) for the full deployment guide. This file is only a short entry point.

## Requirements

- Docker 24+ and Docker Compose v2+
- Git
- Container registry access for production deployments

## Required Secrets

| Secret | Environment Variable | Description |
|--------|----------------------|-------------|
| Database password | `DB_PASSWORD` | PostgreSQL password |
| Redis password | `REDIS_PASSWORD` | Redis password |
| JWT secret | `JWT_SECRET` | JWT signing secret, at least 32 characters |
| Grafana password | `GRAFANA_ADMIN_PASSWORD` | Grafana admin password |

## Start Services

```bash
# Development
cp .env.example .env
docker compose up -d

# Production
cp .env.production.example .env.production
docker compose -f docker-compose.prod.yml --env-file .env.production up -d --build
```

## Verify Services

```bash
curl http://localhost:8080/api/actuator/health
docker compose ps
docker compose logs -f backend
```

## Common Endpoints

| Service | URL |
|---------|-----|
| Backend health | `http://localhost:8080/api/actuator/health` |
| Swagger UI | `http://localhost:8080/api/swagger-ui.html` |
| Grafana | `http://localhost:3000` |
| Prometheus | `http://localhost:9090` |

## Related Documents

- [`../DEPLOYMENT.md`](../DEPLOYMENT.md): full deployment guide
- [`architecture.md`](architecture.md): system architecture
- [`security.md`](security.md): security baseline


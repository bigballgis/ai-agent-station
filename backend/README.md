# AI Agent Platform - Backend

## Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3
- **Security**: Spring Security + JWT
- **Database**: PostgreSQL 16 (with pgvector)
- **Cache**: Redis 7
- **ORM**: Spring Data JPA + Hibernate
- **Migration**: Flyway
- **API Docs**: SpringDoc OpenAPI (Swagger)
- **Monitoring**: Spring Boot Actuator + Prometheus + Grafana
- **Build**: Maven

## Local Development

### Prerequisites

- JDK 17+
- Maven 3.8+
- PostgreSQL 16
- Redis 7
- Docker & Docker Compose (optional)

### Quick Start with Docker Compose

```bash
# Start all services (PostgreSQL, Redis, Prometheus, Grafana, Backend, Frontend)
docker compose up -d

# View logs
docker compose logs -f backend
```

### Manual Setup

1. **Start dependencies**:
   ```bash
   # PostgreSQL
   docker run -d --name ai-postgres -p 5432:5432 -e POSTGRES_PASSWORD=postgres pgvector/pgvector:pg16

   # Redis
   docker run -d --name ai-redis -p 6379:6379 redis:7-alpine
   ```

2. **Configure environment variables** (see `.env.example`):
   ```bash
   cp .env.example .env
   # Edit .env with your values
   ```

3. **Build and run**:
   ```bash
   mvn clean package -DskipTests
   java -jar target/ai-agent-platform-1.0.0.jar
   ```

The API is available at `http://localhost:8080/api`.

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `SERVER_PORT` | Server port | `8080` |
| `DB_PASSWORD` | PostgreSQL password | (required) |
| `SPRING_DATASOURCE_URL` | Database JDBC URL | `jdbc:postgresql://postgres:5432/ai_agent_platform` |
| `REDIS_PASSWORD` | Redis password | (empty) |
| `JWT_SECRET` | JWT signing secret | (required) |
| `JWT_EXPIRATION` | JWT expiration (ms) | `1800000` (30min) |
| `OPENAI_API_KEY` | OpenAI API key | (empty) |
| `QWEN_API_KEY` | Qwen API key | (empty) |
| `LLM_DEFAULT_PROVIDER` | Default LLM provider | `openai` |
| `LOG_LEVEL` | Logging level | `info` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:5173,http://localhost:3000` |

## API Documentation

After starting the application, access the Swagger UI at:

```
http://localhost:8080/api/swagger-ui.html
```

## Project Structure

```
src/main/java/com/aiagent/
  annotation/       # Custom annotations (@OperationLog, @RateLimit, etc.)
  aspect/           # AOP aspects (logging, auditing, rate limiting)
  common/           # Common response wrappers
  config/           # Spring configuration
  controller/       # REST controllers
  dto/              # Data Transfer Objects
  engine/           # Agent execution engine
  entity/           # JPA entities
  exception/        # Exception handling
  gateway/          # API gateway filters
  mcp/              # MCP tool gateway
  repository/       # Data access layer
  security/         # Security configuration & filters
  service/          # Business logic layer
  tenant/           # Multi-tenancy support
  util/             # Utility classes
  websocket/        # WebSocket handlers
```

## Database Migrations

Flyway migrations are located in `src/main/resources/db/migration/`. They run automatically on application startup.

# Configuration & Development

## Environments

Micronaut environment is selected with `MICRONAUT_ENVIRONMENTS`:

- **Default**: `application.yml`
- **Local development**: `application-local.yml`, run with `MICRONAUT_ENVIRONMENTS=local ./gradlew run`
- **Docker**: `application-docker.yml`, set automatically by the compose files

## Environment variables

Copy `.env.example` to `.env` before running Docker Compose. Key variables:

| Variable | Purpose |
|---|---|
| `POSTGRES_*` | Database credentials |
| `APP_PORT` | API port (default 8080) |
| `JVM_MIN_HEAP` / `JVM_MAX_HEAP` | JVM sizing |
| `DB_POOL_SIZE` / `DB_POOL_MIN_IDLE` | HikariCP connection pool |
| `TOTRACKIT_API_KEY` | Optional static API key (see [Security](security.md)) |
| `TOTRACKIT_WEBHOOK_URL` | Optional deadline-missed webhook (see [Notifications](notifications.md)) |
| `TOTRACKIT_PUBLIC_URL` | Public dashboard URL; adds deep links to webhook payloads |
| `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` | Monitoring stack credentials |

## Docker development

```bash
# Start everything (PostgreSQL + Application + Swagger UI)
docker-compose up --build

# Start only PostgreSQL (for local Java development)
docker-compose up -d postgres

# View logs
docker-compose logs -f

# Stop everything
docker-compose down

# Clean reset (removes volumes)
docker-compose down -v
```

Production with monitoring (Prometheus, Grafana, Alertmanager):

```bash
docker-compose -f docker-compose.prod.yml -f docker-compose.monitoring.yml up -d
```

## Service ports

| Service | Port |
|---|---|
| App (API) | 8080 |
| Swagger UI | 8081 |
| Web UI dev server (Vite) | 5173 |
| PostgreSQL | 5432 |
| Prometheus | 9090 |
| Grafana | 3000 |
| Alertmanager | 9093 |

## Running from source

Prerequisites: Java 21, Node.js, Docker.

```bash
git clone https://github.com/plein/ToTrackIt.git
cd ToTrackIt

# Backend
docker-compose up -d postgres
MICRONAUT_ENVIRONMENTS=local ./gradlew run

# Frontend (separate terminal)
cd frontend && npm install && npm run dev   # http://localhost:5173

# Tests
./gradlew test
```

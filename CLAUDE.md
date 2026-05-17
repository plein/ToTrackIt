# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ToTrackIt is an open-source SaaS backend for tracking asynchronous processes and jobs with deadlines, built with **Java 21** and **Micronaut** (lightweight, cloud-native framework). Data is stored in **PostgreSQL 15** with Flyway migrations. Prometheus metrics and health checks are built in.

## Common Commands

```bash
# Build
./gradlew build

# Run locally (with local environment config)
MICRONAUT_ENVIRONMENTS=local ./gradlew run

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.totrackit.service.ProcessServiceTest"

# Regenerate OpenAPI spec (outputs to src/main/resources/public/)
./gradlew copyOpenApiSpec

# Start database only (for hybrid local dev)
docker-compose up -d postgres

# Full stack (app + DB + Swagger UI)
docker-compose up --build

# Production with monitoring (Prometheus, Grafana, Alertmanager)
docker-compose -f docker-compose.prod.yml -f docker-compose.monitoring.yml up -d
```

## Environment Setup

Copy `.env.example` to `.env` before running Docker Compose. Key variables:
- `POSTGRES_*` — database credentials
- `APP_PORT` (default 8080), `JVM_MIN_HEAP` / `JVM_MAX_HEAP`
- `DB_POOL_SIZE`, `DB_POOL_MIN_IDLE` — HikariCP connection pool
- `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` / `GRAFANA_SECRET_KEY`

Multi-environment Micronaut config: `application.yml` (base), `application-local.yml`, `application-docker.yml`. Activate with `MICRONAUT_ENVIRONMENTS=<env>`.

## Architecture

Clean layered architecture: **Controller → Service → Repository → Entity/DTO**.

```
src/main/java/com/totrackit/
├── controller/       # HTTP layer: ProcessController, HealthController, GlobalExceptionHandler
├── service/          # Business logic: ProcessService, MetricsService, HealthService
├── repository/       # JDBC data access: ProcessRepository (custom queries, filtering, pagination)
├── entity/           # DB entity: ProcessEntity, enums ProcessStatus / DeadlineStatus
├── dto/              # Request/response types: NewProcessRequest, ProcessResponse, PagedResult, ProcessFilter
├── mapper/           # ProcessMapper (entity ↔ DTO)
└── interceptor/      # MetricsInterceptor (HTTP request metrics)
src/main/resources/
├── application*.yml  # Micronaut config per environment
└── db/migration/     # Flyway migrations (V1 baseline → V2 schema → V3 indexes)
```

### Key Design Decisions

- **DeadlineStatus is computed at read time** — not persisted. `ProcessService` calculates `ON_TRACK`, `MISSED`, `COMPLETED_ON_TIME`, `COMPLETED_LATE` from stored timestamps.
- **Uniqueness** is enforced via a PostgreSQL partial unique index on `process_id` where `status = 'ACTIVE'` (V2 migration), preventing race conditions at the DB level instead of application level.
- **Tags and context** are stored as JSONB columns for flexibility without schema migrations.
- **Metrics** are recorded in `MetricsService` and wired via `MetricsInterceptor`; Prometheus scrapes `/prometheus`.
- **OpenAPI docs** are auto-generated from annotations and served at `/openapi.yml`; Swagger UI runs on a separate nginx container at port 8081.

### Database Schema

Main table: `processes`
| Column | Type | Notes |
|---|---|---|
| `id` | BIGSERIAL | Internal PK |
| `process_id` | VARCHAR | External identifier |
| `name` | VARCHAR | Display name |
| `status` | VARCHAR | ACTIVE / COMPLETED / FAILED |
| `started_at` | TIMESTAMPTZ | |
| `completed_at` | TIMESTAMPTZ | Nullable |
| `deadline` | TIMESTAMPTZ | Nullable |
| `tags` | JSONB | Flexible tagging |
| `context` | JSONB | Arbitrary metadata |

### Service Ports

| Service | Port |
|---|---|
| App (API) | 8080 |
| Swagger UI | 8081 |
| PostgreSQL | 5432 |
| Prometheus | 9090 |
| Grafana | 3000 |
| Alertmanager | 9093 |

## Testing

Uses **JUnit 5**, **Mockito**, and **Testcontainers** (spins up a real PostgreSQL container for integration and repository tests). Tests live in `src/test/java/com/totrackit/` mirroring the main package structure.

- Unit tests: `ProcessServiceTest`, `MetricsServiceTest`, `ProcessMapperTest`
- Integration tests: `ProcessControllerIntegrationTest`, `MetricsIntegrationTest`
- Repository tests: `ProcessRepositoryTest` (runs against Testcontainers PostgreSQL)

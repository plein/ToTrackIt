# ToTrackIt

**Open-source SaaS platform to track, monitor, and analyze asynchronous processes.**

ToTrackIt helps organizations gain visibility into critical business operations, ensuring deadlines are met, incidents are reduced, and teams have actionable insights for continuous improvement.

---

## ✨ Features

* **Process Tracking & Deadlines**
  Register processes with unique IDs, deadlines, and metadata. Track them in real time.

* **Notifications**
  Get alerted via email or webhooks when deadlines approach or are breached.

* **Tags & Contextual Data**
  Add tags (e.g. `environment=production`, `priority=high`) or custom context (e.g. `customerId: 1234`) for granular analysis.

* **Metrics & Analytics**
  Measure average completion times, missed deadlines %, and latency distributions per namespace, process, or tag.

* **Secure Access Control**
  Namespaces and API keys to isolate workloads. Integrate with Cognito/Auth0 for authentication.

* **APIs + UI**
  Use a simple UI or robust APIs to register, complete, and analyze processes.

---

## 🚀 Getting Started

### Prerequisites

* Java 21 (Micronaut backend)
* Node.js / React (UI)
* Docker & Docker Compose

### Clone & Build

```bash
git clone https://github.com/plein/ToTrackIt.git
cd totrackit

# Build backend
./gradlew build

# Run tests
./gradlew test
```

### Quick Start with Docker Compose

```bash
# Start the complete environment (PostgreSQL + Application)
docker-compose up --build

# Or start just the database for local development
docker-compose up -d postgres

# Then run the application locally
MICRONAUT_ENVIRONMENTS=local ./gradlew run
```

### Manual Database Setup

```bash
# Alternative: Start PostgreSQL manually
docker run --name totrackit-postgres \
  -e POSTGRES_DB=totrackit \
  -e POSTGRES_USER=totrackit \
  -e POSTGRES_PASSWORD=totrackit \
  -p 5432:5432 -d postgres:15-alpine

# Run backend locally
./gradlew run
```

The API will be available at:
`http://localhost:8080/v1/`

The Swagger UI will be available at:
`http://localhost:8081/` (API documentation and testing)

The UI will be available at:
`http://localhost:3000/` (coming in Phase 2)

---

## 🐳 Docker Development

### Using Docker Compose (Recommended)

The project includes a complete Docker Compose setup for local development:

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

### Services Available

When running `docker-compose up`, you'll have access to:

- **API Server**: `http://localhost:8080/v1/` - Your ToTrackIt REST API
- **Swagger UI**: `http://localhost:8081/` - Interactive API documentation and testing interface
- **PostgreSQL**: `localhost:5432` - Database (accessible for local development)

### Environment Configuration

- **Docker environment**: Uses `application-docker.yml`
- **Local development**: Uses `application-local.yml` 
- **Default**: Uses `application.yml`

Set the environment with:
```bash
MICRONAUT_ENVIRONMENTS=docker ./gradlew run
# or
MICRONAUT_ENVIRONMENTS=local ./gradlew run
```

### Testing Docker Setup

```bash
# Validate Docker configuration
./validate-docker-setup.sh

# Full environment test (requires Docker)
./docker/test-environment.sh
```

---

## 📖 API Overview

ToTrackIt exposes a **REST API** (OpenAPI 3.1). Full spec: [`api.yaml`](./api.yaml).

### Interactive API Documentation

The easiest way to explore and test the API is through the **Swagger UI**:

1. Start the services: `docker-compose up`
2. Open `http://localhost:8081/` in your browser
3. Browse all endpoints with detailed documentation
4. Test API calls directly from the interface
5. Use the "Authorize" button to add your API keys

### Key Endpoints

* `POST /processes/{name}` → Start a process
* `POST /processes/{name}/{id}/complete` → Mark process as completed
* `GET /processes/metrics` → Get metrics & deadline breaches
* `POST /notifications` → Configure alerts

### Example (register process):

```bash
curl -X POST "http://localhost:8080/v1/processes/dataImport" \
  -H "X-API-KEY: ns_abc123" \
  -H "Content-Type: application/json" \
  -d '{
        "id": "batch42",
        "deadline": 1699999999,
        "tags": [{ "key": "env", "value": "prod" }],
        "context": { "customerId": "C1234" }
      }'
```

**Tip**: Instead of using curl, try the same request in Swagger UI at `http://localhost:8081/` for a better development experience!

---

## �️ Dautabase

### Schema Overview

ToTrackIt uses **PostgreSQL** with **Flyway** for database migrations. The database schema is designed for high performance with proper indexing for common query patterns.

### Core Tables

#### `processes` Table
The main table storing all process tracking data:

```sql
-- Key columns:
id              BIGSERIAL PRIMARY KEY
process_id      VARCHAR(50)           -- User-defined process identifier
name            VARCHAR(100)          -- Process name (e.g., "dataImport")
status          VARCHAR(20)           -- ACTIVE, COMPLETED, FAILED
started_at      TIMESTAMP WITH TIME ZONE
completed_at    TIMESTAMP WITH TIME ZONE
deadline        TIMESTAMP WITH TIME ZONE
tags            JSONB                 -- Flexible tagging system
context         JSONB                 -- Custom metadata
```

### Database Access

#### Connect to PostgreSQL

```bash
# Using Docker Compose
docker-compose exec postgres psql -U totrackit -d totrackit

# Or connect directly
psql -h localhost -p 5432 -U totrackit -d totrackit
```

#### Useful Queries

```sql
-- View all active processes
SELECT name, process_id, started_at, deadline 
FROM processes 
WHERE status = 'ACTIVE' 
ORDER BY started_at DESC;

-- Check processes approaching deadlines (next 24 hours)
SELECT name, process_id, deadline, 
       EXTRACT(EPOCH FROM (deadline - NOW()))/3600 as hours_remaining
FROM processes 
WHERE status = 'ACTIVE' 
  AND deadline IS NOT NULL 
  AND deadline < NOW() + INTERVAL '24 hours'
ORDER BY deadline;

-- Process completion metrics
SELECT 
    name,
    COUNT(*) as total_processes,
    AVG(EXTRACT(EPOCH FROM (completed_at - started_at))) as avg_duration_seconds,
    COUNT(*) FILTER (WHERE completed_at > deadline) as missed_deadlines
FROM processes 
WHERE status = 'COMPLETED' 
GROUP BY name;

-- Query by tags (using JSONB operators)
SELECT * FROM processes 
WHERE tags @> '{"environment": "production"}';

-- Query by context
SELECT * FROM processes 
WHERE context @> '{"priority": "high"}';
```

### Flyway Migrations

Database schema is managed through Flyway migrations in `src/main/resources/db/migration/`:

- **V1__Initial_baseline.sql**: Baseline migration
- **V2__Create_processes_table.sql**: Core processes table with constraints and triggers
- **V3__Add_indexes.sql**: Performance indexes for common queries

#### Migration Commands

```bash
# Migrations run automatically on application startup
# To check migration status:
docker-compose exec app ./gradlew flywayInfo

# To manually run migrations (if needed):
docker-compose exec app ./gradlew flywayMigrate
```

### Performance Features

- **Unique constraint**: Prevents duplicate active processes (same name + process_id)
- **Automatic timestamps**: `updated_at` automatically maintained via triggers
- **JSONB indexes**: GIN indexes on `tags` and `context` for fast JSON queries
- **Composite indexes**: Optimized for common filtering patterns
- **Partial indexes**: Efficient indexing for status-specific queries

---

## 🛡 Security

* All API access is authenticated via **API Keys** (namespace or admin scope).
* Supports OAuth2 via Cognito/Auth0.
* Data encrypted in transit & at rest.

---

## 🗺️ Roadmap

### Phase 0 — Core Process API (local-only)

* In-memory process storage
* CRUD + metrics endpoints for processes
* Minimal testing and docs

### Phase 1 — Kubernetes-ready Service

* Docker image + Helm chart/Kustomize
* Postgres support with migrations
* Basic observability (logs + Prometheus metrics)

### Phase 2 — UI (MVP)

* Local web UI for starting/viewing/completing processes
* Basic metrics dashboard

### Phase 3 — Platform Features

* Namespaces, admin API, users, API keys
* Notification channels (email/webhook)
* OAuth2 (Cognito/Auth0)

### Later / Nice-to-have

* Slack/OpsGenie/Teams channels
* OpenTelemetry exporter
* Advanced analytics (percentiles, time-series)
* SDKs (Java/TS/Go)
---

## 🤝 Contributing

We welcome contributions! Please open issues or PRs.

1. Fork the repo
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit changes and push
4. Open a PR

---

## 📜 License

[Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

---

## 🙌 Acknowledgments

ToTrackIt was inspired by real-world experience at AWS, where teams struggled with visibility into async processes. This project exists to solve that gap — for everyone.

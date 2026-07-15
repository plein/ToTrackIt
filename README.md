# ToTrackIt

**Open-source SaaS platform to track, monitor, and analyze asynchronous processes.**

ToTrackIt helps organizations gain visibility into critical business operations, ensuring deadlines are met, incidents are reduced, and teams have actionable insights for continuous improvement.

---

## ✨ Features

* **Process Tracking & Deadlines**
  Register processes with unique IDs, deadlines, and metadata. Track them in real time.

* **Webhook Notifications**
  Get a webhook notification when a process misses its deadline. (Email and other channels are on the roadmap.)

* **Tags & Contextual Data**
  Add tags (e.g. `environment=production`, `priority=high`) or custom context (e.g. `customerId: 1234`) for granular analysis.

* **Metrics & Analytics**
  Measure average completion times, missed deadlines, and completion trends per process — in the UI and via Prometheus/Grafana.

* **APIs + UI**
  Use the built-in web UI or the REST API (with OpenAPI docs) to register, complete, and analyze processes.

* **Self-Hosted & Open Source**
  Run the full stack with Docker Compose. Optional static API key for API access (see [Security](#-security)).

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

## 📖 API Documentation

ToTrackIt provides comprehensive API documentation through Swagger/OpenAPI, generated directly from the code to ensure it's always up-to-date with the actual implementation.

### Accessing API Documentation

**Option 1: Built-in Swagger UI (Recommended)**
When running the application locally:
```bash
./gradlew run
```
Visit: http://localhost:8080/swagger-ui

**Option 2: Docker Compose with Static UI**
```bash
docker-compose up -d
```
Visit: http://localhost:8081 (uses static api.yaml file)

**Option 3: Raw OpenAPI Specification**
Get the machine-readable OpenAPI spec:
```bash
curl http://localhost:8080/openapi.yml
```

### Key Features of Generated Documentation

- **Always Current**: Generated from actual controller code and annotations
- **Interactive**: Test API endpoints directly from the browser
- **Comprehensive**: Includes request/response schemas, validation rules, and examples
- **Type-Safe**: Reflects actual Java types and validation constraints

### Updating Documentation

The OpenAPI specification is automatically generated during compilation. To update:

```bash
./gradlew copyOpenApiSpec
```

This copies the generated spec from `build/classes/java/main/META-INF/swagger/` to `src/main/resources/openapi.yml` for runtime access.

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

The web UI dev server (Vite) will be available at:
`http://localhost:5173/` (run `cd frontend && npm install && npm run dev`)

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
* `GET /processes` → List processes (filtering + pagination)
* `GET /processes/{name}/{id}` → Get a single process
* `PUT /processes/{name}/{id}/complete` → Mark process as completed (or failed)
* `DELETE /processes/{name}/{id}` → Delete a process

### Example (register process):

```bash
curl -X POST "http://localhost:8080/processes/dataImport" \
  -H "Content-Type: application/json" \
  -d '{
        "id": "batch42",
        "deadline": 1699999999,
        "tags": [{ "key": "env", "value": "prod" }],
        "context": { "customerId": "C1234" }
      }'
```

If you configured an API key (see [Security](#-security)), add `-H "X-API-KEY: <your key>"`.

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

**ToTrackIt is designed to run inside a trusted network.** By default the API is unauthenticated — anyone who can reach it can create, complete, and delete processes. Do **not** expose it directly to the public internet.

Deployment options, from simplest to strongest:

1. **Private network only (default).** Run ToTrackIt behind your VPN/firewall and rely on network-level access control.
2. **Static API key.** Set the `TOTRACKIT_API_KEY` environment variable and every `/processes` request must carry a matching `X-API-KEY` header. Health, metrics, and API docs endpoints stay open. This is a single shared key for the whole deployment — suitable for one team, not a user-management system.
3. **Reverse proxy.** Terminate TLS and add your own auth (basic auth, OIDC proxy, etc.) in front of ToTrackIt.

Multi-tenant namespaces, per-user API keys, and SSO are planned for a future managed/enterprise offering and are intentionally not part of the open-source core.

---

## 🔔 Notifications

Set `TOTRACKIT_WEBHOOK_URL` to enable deadline-breach notifications. A background scanner (every 60s by default, tunable via `totrackit.notification-scan-interval`) finds active processes past their deadline and sends each one a single JSON POST:

```json
{
  "event": "process.deadline_missed",
  "name": "dataImport",
  "id": "batch42",
  "started_at": 1699990000,
  "deadline": 1699999999,
  "tags": [{ "key": "env", "value": "prod" }],
  "context": { "customerId": "C1234" }
}
```

Any 2xx response marks the process as notified; failed deliveries are retried on the next scan. Point it at Slack (via a bridge), your incident tooling, or any HTTP endpoint.

---

## 🗺️ Roadmap

### Done

* Core process API: create, get, list (filter/paginate), complete/fail, delete
* PostgreSQL with Flyway migrations; Docker Compose for dev and prod
* Observability: health endpoints, Prometheus metrics, Grafana/Alertmanager stack
* Web UI: dashboard, per-name rollups, tags, metrics
* Webhook notifications on missed deadlines
* Optional static API key

### Next (open-source core)

* Email notification channel
* Helm chart / Kustomize for Kubernetes
* Advanced analytics (percentiles, time-series)
* SDKs (Java/TS/Go)
* OpenTelemetry exporter

### Future (managed / enterprise)

* Namespaces & multi-tenancy
* Per-user API keys, admin API
* OAuth2/SSO (Cognito/Auth0)
* Slack/OpsGenie/Teams channels
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

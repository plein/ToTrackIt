# ToTrackIt

[![CI](https://github.com/plein/ToTrackIt/actions/workflows/ci.yml/badge.svg)](https://github.com/plein/ToTrackIt/actions/workflows/ci.yml)
[![Docker](https://github.com/plein/ToTrackIt/actions/workflows/publish-image.yml/badge.svg)](https://github.com/plein/ToTrackIt/actions/workflows/publish-image.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)

**Open-source deadline tracking and root-cause analytics for asynchronous business processes.**

Every company runs async processes with implicit SLOs — account activations, payment settlements, KYC reviews, batch imports. When one silently gets stuck, the customer finds out before you do. ToTrackIt makes those processes first-class: register each run with two API calls (start + complete), give it a deadline, and you get real-time tracking, SLO metrics, and — the part your APM can't do — **root-cause analysis by business tags**.

**ToTrackIt is the diagnosis layer, not the pager.** Keep alerting in the stack you already trust: ToTrackIt exposes deadline-shaped Prometheus metrics that plug straight into Datadog monitors, Prometheus alerts, and metric-based SLOs. When an alert fires, its deep link lands on the process page in ToTrackIt — and the impacted-tags view tells you in one glance that all 12 stuck activations share `country:DE` and `kyc:provider-x`, and that DE's p90 latency is 9× everyone else's.

```
your process ──2 API calls──▶ ToTrackIt ──/prometheus──▶ Datadog / Prometheus (alerting, SLOs)
                                  ▲                                  │ alert fires
                                  └────────── deep link ─────────────┘
                          "all problems are country:DE" — impacted tags, latency by tag
```

---

## ✨ Features

* **Process Tracking & SLO Deadlines**
  Register runs with unique IDs, deadlines, tags, and context. Deadline status (`ON_TRACK`, `MISSED`, `COMPLETED_ON_TIME`, `COMPLETED_LATE`) is computed in real time.

* **Works With Your Alerting** — Datadog, Prometheus, Grafana
  Deadline-aware metrics (`overdue_current` gauge, missed/on-time/late counters) power stuck-process monitors and metric-based SLOs in the tools your on-call already watches. See [Metrics, Prometheus & Datadog](#-metrics-prometheus--datadog).

* **Root Cause by Tags**
  Tag runs with business dimensions (`country`, `channel`, `provider`, `customerId`…). The impacted-tags view and `GET /analytics/tags` show which segment the overdue, late, and failed runs concentrate in — and completion latency (avg/p50/p90/p99) per tag, so slow segments stand out.

* **Per-Process Pages & Alert Deep Links**
  Every process name has a shareable page (`/?name=account-activation`) with all runs, a period picker, impact and latency breakdowns. Webhook notifications carry a URL that lands the on-call directly on the affected process.

* **Webhook Notifications**
  A JSON POST fires when a process misses its deadline, with tags, context, and the dashboard deep link. (Email and other channels are on the roadmap.)

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
* `GET /analytics/tags` → Per-tag breakdown of deadline outcomes (which country/locale/segment the problems are concentrated in)

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
  "context": { "customerId": "C1234" },
  "url": "https://totrackit.internal.example.com/?process=dataImport/batch42"
}
```

Any 2xx response marks the process as notified; failed deliveries are retried on the next scan. Point it at Slack (via a bridge), your incident tooling, or any HTTP endpoint.

The `url` field is included when `TOTRACKIT_PUBLIC_URL` is set to the public base URL of the dashboard — it deep-links straight to the impacted process, so whoever receives the alert is one click away from the process page and the impacted-tags view.

The deadline scanner runs regardless of webhook configuration; it also feeds the `totrackit_processes_deadline_missed_total` metric (see below).

---

## 📊 Metrics, Prometheus & Datadog

ToTrackIt exposes Prometheus/OpenMetrics at `/prometheus`. Beyond HTTP and DB metrics, the deadline-aware metrics are designed to plug ToTrackIt into whatever alerting your team already pages on:

| Metric | Type | Labels | Meaning |
|---|---|---|---|
| `totrackit_processes_overdue_current` | gauge | `process_name` | Active processes currently past their deadline (updated every 30s) |
| `totrackit_processes_deadline_missed_total` | counter | `process_name` | Deadline breaches, counted once per process |
| `totrackit_processes_completed_on_time_total` | counter | `process_name` | Completions within the deadline |
| `totrackit_processes_completed_late_total` | counter | `process_name` | Completions after the deadline |
| `totrackit_active_processes_current` | gauge | — | All currently active processes |

### Prometheus alert example

```yaml
- alert: ProcessOverdue
  expr: totrackit_processes_overdue_current > 0
  for: 1m
  annotations:
    summary: "{{ $labels.process_name }}: {{ $value }} process(es) past deadline"
```

### Datadog

The Datadog Agent scrapes `/prometheus` natively via its OpenMetrics integration — add to `conf.d/openmetrics.d/conf.yaml`:

```yaml
instances:
  - openmetrics_endpoint: http://<totrackit-host>:8080/prometheus
    namespace: totrackit
    metrics:
      - totrackit_.*
```

Typical monitors:

* **Stuck processes** — metric monitor on `totrackit.processes_overdue_current` `> 0` by `process_name`. Fires while anything is past its deadline; resolves when the backlog clears.
* **SLO / on-time rate** — create a Datadog metric-based SLO with good events = `completed_on_time_total` and total events = `completed_on_time_total + completed_late_total + deadline_missed_total`. This gives you error budgets and burn-rate alerts on e.g. "99% of account activations complete within 1 hour".

When a monitor fires, the dashboard's **Impacted tags** panel (and `GET /analytics/tags`) shows which segment — country, locale, provider — the overdue and late processes are concentrated in.

---

## 🗺️ Roadmap

### Done

* Core process API: create, get, list (filter/paginate), complete/fail, delete
* PostgreSQL with Flyway migrations; Docker Compose for dev and prod
* Observability: health endpoints, Prometheus metrics, Grafana/Alertmanager stack
* Web UI: dashboard, per-name rollups, tags, metrics
* Webhook notifications on missed deadlines (with dashboard deep-links)
* Deadline-aware metrics for Prometheus/Datadog monitors and SLOs
* Tag-impact analytics: which segment the problems concentrate in, with completion latency (avg/p50/p90/p99) overall and per tag
* Per-name process pages (`/?name=...`) with period picker, impact, and latency breakdowns
* Optional static API key

### Next (open-source core)

* Pre-deadline warning events (notify at e.g. 75% of the SLO, before the customer notices)
* Webhook signing (HMAC) and per-namespace webhook routing
* Time-series analytics (trends over time, not just windows)
* Email notification channel
* Helm chart / Kustomize for Kubernetes
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

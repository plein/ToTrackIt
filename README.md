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

The UI will be available at:
`http://localhost:3000/` (coming in Phase 2)

---

## 🐳 Docker Development

### Using Docker Compose (Recommended)

The project includes a complete Docker Compose setup for local development:

```bash
# Start everything
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

ToTrackIt exposes a **REST API** (OpenAPI 3.1). Full spec: [`swagger.yaml`](./api.yaml).

Some key endpoints:

* `POST /processes/{name}` → Start a process
* `POST /processes/{name}/{id}/complete` → Mark process as completed
* `GET /processes/metrics` → Get metrics & deadline breaches
* `POST /notifications` → Configure alerts

Example (register process):

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

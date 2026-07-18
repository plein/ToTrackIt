# Contributing to ToTrackIt

Thanks for your interest! Issues and pull requests are welcome.

## Getting set up

Prerequisites: Java 21, Node.js, Docker.

```bash
git clone https://github.com/plein/ToTrackIt.git
cd ToTrackIt

# Backend: start the database, run the app
docker-compose up -d postgres
MICRONAUT_ENVIRONMENTS=local ./gradlew run

# Frontend
cd frontend && npm install && npm run dev
```

## Running tests

```bash
./gradlew test          # backend (some tests need Docker for Testcontainers PostgreSQL)
cd frontend
npm run lint            # frontend lint
npm run build           # frontend type-check + build
```

## Making changes

1. Fork the repo and create a feature branch (`git checkout -b feature/my-feature`)
2. Make your change, with tests for new behavior
3. Make sure `./gradlew test` and `npm run lint && npm run build` pass
4. Open a PR describing what the change does and why

A few conventions:

- Backend follows Controller → Service → Repository layering (see `CLAUDE.md` for the architecture map)
- New database columns need a Flyway migration **and** a mirror in `src/test/resources/schema.sql` (H2 test schema)
- Metric names use the `totrackit_` prefix with a `process_name` label where applicable

## Reporting bugs / requesting features

Open a [GitHub issue](https://github.com/plein/ToTrackIt/issues) with steps to reproduce (for bugs) or the problem you're trying to solve (for features).

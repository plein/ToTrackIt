# Database

ToTrackIt uses **PostgreSQL 15** with **Flyway** migrations. The schema is designed for the common query patterns: active-process lookups, deadline scans, and JSONB tag filtering.

## Schema

Main table: `processes`

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

Performance features:

- **Partial unique index**: prevents duplicate active processes (same `name` + `process_id`) at the database level, eliminating race conditions
- **Automatic timestamps**: `updated_at` maintained via triggers
- **JSONB GIN indexes** on `tags` and `context` for fast JSON queries
- **Composite and partial indexes** for common filtering patterns

## Connecting

```bash
# Using Docker Compose
docker-compose exec postgres psql -U totrackit -d totrackit

# Or connect directly
psql -h localhost -p 5432 -U totrackit -d totrackit
```

## Useful queries

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
WHERE tags @> '[{"key": "env", "value": "prod"}]';
```

## Flyway migrations

Schema is managed through migrations in `src/main/resources/db/migration/`. Migrations run automatically on application startup.

```bash
# Check migration status
docker-compose exec app ./gradlew flywayInfo

# Manually run migrations (if needed)
docker-compose exec app ./gradlew flywayMigrate
```

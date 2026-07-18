# API Reference

ToTrackIt exposes a **REST API** (OpenAPI 3.1). The specification is generated from the code, so it is always current with the actual implementation.

## Key endpoints

* `POST /processes/{name}` → Start a process
* `GET /processes` → List processes (filtering + pagination)
* `GET /processes/{name}/{id}` → Get a single process
* `PUT /processes/{name}/{id}/complete` → Mark process as completed (or failed)
* `DELETE /processes/{name}/{id}` → Delete a process
* `GET /analytics/tags` → Per-tag breakdown of deadline outcomes and completion latency (avg/p50/p90/p99), overall and per tag

## Example: track a process

```bash
# Start it with a deadline and business tags
curl -X POST "http://localhost:8080/processes/dataImport" \
  -H "Content-Type: application/json" \
  -d '{
        "id": "batch42",
        "deadline": 1699999999,
        "tags": [{ "key": "env", "value": "prod" }],
        "context": { "customerId": "C1234" }
      }'

# Complete it when it finishes
curl -X PUT "http://localhost:8080/processes/dataImport/batch42/complete" \
  -H "Content-Type: application/json" \
  -d '{ "status": "COMPLETED" }'
```

If you configured an API key (see [Security](security.md)), add `-H "X-API-KEY: <your key>"`.

## Interactive documentation (Swagger UI)

**Option 1: Built-in Swagger UI**

```bash
./gradlew run
```

Visit http://localhost:8080/swagger-ui

**Option 2: Docker Compose static UI**

```bash
docker-compose up -d
```

Visit http://localhost:8081 to browse all endpoints, test calls from the browser, and use the "Authorize" button to add your API key.

**Option 3: Raw OpenAPI specification**

```bash
curl http://localhost:8080/openapi.yml
```

## Regenerating the spec

The OpenAPI specification is generated during compilation. To refresh the committed copy:

```bash
./gradlew copyOpenApiSpec
```

This copies the generated spec from `build/classes/java/main/META-INF/swagger/` to `src/main/resources/openapi.yml`.

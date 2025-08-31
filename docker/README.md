# ToTrackIt Docker Setup

This directory contains enhanced Docker configurations for running ToTrackIt in various environments. The setup provides multiple compose files for different use cases, from development to production deployment with full observability stack.

## Quick Start

### Development (Full Stack)
```bash
# Start all services including database, application, and Swagger UI
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Development (Database Only)
```bash
# Start only PostgreSQL for hybrid development
docker-compose -f docker-compose.dev.yml up -d postgres

# Run application natively while using containerized database
./gradlew run
```

### Production Deployment
```bash
# Copy and configure environment variables
cp .env.example .env
# Edit .env with your production values

# Start production stack
docker-compose -f docker-compose.prod.yml up -d

# With monitoring stack
docker-compose -f docker-compose.prod.yml -f docker-compose.monitoring.yml up -d
```

## Docker Compose Files

### `docker-compose.yml` (Default)
- **Purpose**: Full development environment
- **Services**: PostgreSQL, ToTrackIt App, Swagger UI
- **Features**: 
  - Improved health checks
  - Resource limits
  - Better dependency management
  - Structured logging

### `docker-compose.dev.yml`
- **Purpose**: Development workflow with optional services
- **Services**: PostgreSQL (always), Redis (optional), App (optional)
- **Features**:
  - Database-only mode for hybrid development
  - Debug port exposure (5005)
  - Hot reload support (when available)
  - Development-specific logging

### `docker-compose.prod.yml`
- **Purpose**: Production-ready deployment
- **Services**: PostgreSQL, ToTrackIt App, Nginx (optional)
- **Features**:
  - Environment variable configuration
  - Resource limits and reservations
  - Security hardening
  - Production logging
  - Horizontal scaling support

### `docker-compose.monitoring.yml`
- **Purpose**: Observability and monitoring stack
- **Services**: Prometheus, Grafana, Alertmanager, Node Exporter, cAdvisor
- **Features**:
  - Metrics collection and visualization
  - Alerting rules
  - System and container monitoring
  - Pre-configured dashboards

## Environment Configuration

### Development
No additional configuration required. Uses default values suitable for local development.

### Production
1. Copy `.env.example` to `.env`
2. Update the following critical values:
   - `POSTGRES_PASSWORD`: Secure database password
   - `GRAFANA_ADMIN_PASSWORD`: Grafana admin password
   - `GRAFANA_SECRET_KEY`: Random secret key for Grafana
   - `JVM_MAX_HEAP`: Adjust based on available memory
   - `DB_POOL_SIZE`: Adjust based on expected load

### Key Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `POSTGRES_PASSWORD` | Database password | - | Yes (prod) |
| `APP_PORT` | Application port | 8080 | No |
| `JVM_MAX_HEAP` | Maximum JVM heap size | 512m | No |
| `METRICS_ENABLED` | Enable Prometheus metrics | true | No |
| `LOG_LEVEL` | Application log level | INFO | No |

## Service Profiles

Use Docker Compose profiles to selectively start services:

```bash
# Start with caching (Redis)
docker-compose -f docker-compose.dev.yml --profile cache up -d

# Start with application container
docker-compose -f docker-compose.dev.yml --profile app up -d

# Start with Nginx reverse proxy
docker-compose -f docker-compose.prod.yml --profile nginx up -d

# Start with alerting
docker-compose -f docker-compose.monitoring.yml --profile alerting up -d

# Start with system metrics
docker-compose -f docker-compose.monitoring.yml --profile system-metrics up -d
```

## Health Checks

All services include comprehensive health checks:

- **PostgreSQL**: `pg_isready` command
- **Application**: HTTP health endpoint (`/health`)
- **Swagger UI**: HTTP availability check
- **Prometheus**: Built-in health endpoint
- **Grafana**: API health check

## Monitoring and Observability

### Accessing Services

| Service | URL | Purpose |
|---------|-----|---------|
| ToTrackIt API | http://localhost:8080 | Main application |
| Swagger UI | http://localhost:8081 | API documentation |
| Prometheus | http://localhost:9090 | Metrics collection |
| Grafana | http://localhost:3000 | Metrics visualization |
| Alertmanager | http://localhost:9093 | Alert management |

### Default Credentials

- **Grafana**: admin / (see GRAFANA_ADMIN_PASSWORD in .env)

### Metrics Available

- HTTP request metrics (duration, count, status codes)
- Database connection pool metrics
- JVM metrics (memory, GC, threads)
- System metrics (CPU, memory, disk)
- Container metrics (resource usage)

## Security Features

### Production Security
- Non-root user execution
- Read-only containers where possible
- Resource limits and reservations
- Network isolation
- Security headers via Nginx
- Rate limiting

### Development Security
- Isolated networks
- Health check validation
- Secure default configurations

## Troubleshooting

### Common Issues

1. **Services fail to start**
   ```bash
   # Check service logs
   docker-compose logs [service-name]
   
   # Check health status
   docker-compose ps
   ```

2. **Database connection issues**
   ```bash
   # Verify PostgreSQL is healthy
   docker-compose exec postgres pg_isready -U totrackit -d totrackit
   
   # Check database logs
   docker-compose logs postgres
   ```

3. **Application startup issues**
   ```bash
   # Check application logs
   docker-compose logs app
   
   # Verify health endpoint
   curl http://localhost:8080/health
   ```

4. **Memory issues**
   ```bash
   # Check resource usage
   docker stats
   
   # Adjust JVM heap size in .env
   JVM_MAX_HEAP=1g
   ```

### Performance Tuning

1. **Database Performance**
   - Adjust `POSTGRES_SHARED_BUFFERS` based on available memory
   - Tune `DB_POOL_SIZE` based on expected concurrent connections
   - Monitor connection pool metrics in Grafana

2. **Application Performance**
   - Adjust `JVM_MAX_HEAP` based on available memory
   - Monitor GC metrics and adjust GC settings if needed
   - Scale horizontally using `APP_REPLICAS`

3. **System Resources**
   - Monitor resource usage via cAdvisor and Node Exporter
   - Adjust Docker resource limits based on actual usage
   - Use SSD storage for better I/O performance

## Development Workflows

### Hybrid Development (Recommended)
1. Start database only: `docker-compose -f docker-compose.dev.yml up -d postgres`
2. Run application natively: `./gradlew run`
3. Access application at http://localhost:8080

### Full Docker Development
1. Start all services: `docker-compose up -d`
2. Access application at http://localhost:8080
3. For debugging, use the dev profile with debug port: `docker-compose -f docker-compose.dev.yml --profile app up -d`

### Testing
1. Start test environment: `docker-compose -f docker-compose.dev.yml up -d postgres`
2. Run tests: `./gradlew test`
3. For integration tests, ensure database is available

## Maintenance

### Backup and Restore
```bash
# Backup database
docker-compose exec postgres pg_dump -U totrackit totrackit > backup.sql

# Restore database
docker-compose exec -T postgres psql -U totrackit totrackit < backup.sql
```

### Updates
```bash
# Pull latest images
docker-compose pull

# Restart services with new images
docker-compose up -d
```

### Cleanup
```bash
# Remove stopped containers and unused images
docker system prune

# Remove volumes (WARNING: This will delete data)
docker-compose down -v
```
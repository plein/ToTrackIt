# Docker Setup for ToTrackIt

This directory contains Docker configuration files for running ToTrackIt locally.

## Prerequisites

- Docker Desktop or Docker Engine
- Docker Compose

## Quick Start

1. **Start the PostgreSQL database:**
   ```bash
   docker-compose up -d postgres
   ```

2. **Wait for database to be healthy:**
   ```bash
   docker-compose ps
   ```
   Wait until postgres shows "healthy" status.

3. **Build and start the application:**
   ```bash
   docker-compose up --build app
   ```

4. **Or start everything together:**
   ```bash
   docker-compose up --build
   ```

## Testing Connectivity

### Database Connectivity Test
```bash
# Test PostgreSQL connection
docker-compose exec postgres psql -U totrackit -d totrackit -c "SELECT health_check();"
```

### Application Health Check
```bash
# Test application health (once running)
curl http://localhost:8080/health
```

### Full Environment Test
```bash
# Start services
docker-compose up -d

# Check all services are running
docker-compose ps

# Check logs
docker-compose logs postgres
docker-compose logs app

# Test database connection
docker-compose exec postgres psql -U totrackit -d totrackit -c "\dt"

# Stop services
docker-compose down
```

## Environment Variables

The following environment variables are configured in docker-compose.yml:

### PostgreSQL
- `POSTGRES_DB=totrackit`
- `POSTGRES_USER=totrackit`
- `POSTGRES_PASSWORD=totrackit`

### Application
- `DATASOURCES_DEFAULT_URL=jdbc:postgresql://postgres:5432/totrackit`
- `DATASOURCES_DEFAULT_USERNAME=totrackit`
- `DATASOURCES_DEFAULT_PASSWORD=totrackit`
- `MICRONAUT_ENVIRONMENTS=docker`

## Volumes

- `postgres_data`: Persistent PostgreSQL data
- `./logs:/app/logs`: Application logs mounted to host

## Ports

- PostgreSQL: `5432:5432`
- Application: `8080:8080`

## Troubleshooting

### Database Connection Issues
```bash
# Check if PostgreSQL is accepting connections
docker-compose exec postgres pg_isready -U totrackit

# View PostgreSQL logs
docker-compose logs postgres

# Connect to database manually
docker-compose exec postgres psql -U totrackit -d totrackit
```

### Application Issues
```bash
# View application logs
docker-compose logs app

# Restart application
docker-compose restart app

# Rebuild application
docker-compose up --build app
```

### Clean Reset
```bash
# Stop and remove all containers and volumes
docker-compose down -v

# Remove built images
docker-compose down --rmi all

# Start fresh
docker-compose up --build
```
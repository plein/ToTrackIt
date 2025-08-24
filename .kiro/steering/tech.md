# Technology Stack

## Backend
- **Language**: Java 21
- **Framework**: Micronaut
- **Database**: 
  - Phase 0: In-memory storage
  - Phase 1+: PostgreSQL with migrations
- **API**: REST with OpenAPI 3.1 specification
- **Build System**: Gradle

## Frontend
- **Language**: JavaScript/TypeScript
- **Framework**: React
- **Runtime**: Node.js

## Infrastructure & Deployment
- **Containerization**: Docker
- **Orchestration**: Kubernetes (Helm charts/Kustomize)
- **Development**: Docker Compose for local setup

## Authentication & Security
- **API Authentication**: API Keys (namespace and admin scoped)
- **OAuth2**: Cognito/Auth0 integration
- **Security**: Data encryption in transit and at rest

## Observability
- **Logging**: Standard application logs
- **Metrics**: Prometheus metrics export
- **Monitoring**: Basic observability stack

## Common Commands

### Backend Development
```bash
# Build the application
./gradlew build

# Run locally
./gradlew run

# Run tests
./gradlew test
```

### Local Development
```bash
# Clone and setup
git clone https://github.com/plein/ToTrackIt.git
cd totrackit

# Start with Docker Compose (when available)
docker-compose up -d
```

### API Access
- **Backend API**: http://localhost:8080/v1/
- **Frontend UI**: http://localhost:3000/
- **API Documentation**: Available via OpenAPI spec in api.yaml

## Development Standards
- Follow Java 21 best practices
- Use Micronaut conventions and annotations
- Maintain OpenAPI specification accuracy
- Implement proper error handling and validation
- Include comprehensive logging for debugging
- Use optimistic locking, avoid transactions when possible
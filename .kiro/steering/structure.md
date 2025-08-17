# Project Structure

## Repository Organization

```
totrackit/
├── api.yaml                 # OpenAPI 3.1 specification
├── README.md               # Project documentation
├── LICENSE                 # Apache 2.0 license
├── .gitignore             # Git ignore rules
├── .kiro/                 # Kiro AI assistant configuration
│   └── steering/          # AI steering rules
└── .vscode/               # VS Code settings
```

## API Specification
- **api.yaml**: Complete OpenAPI 3.1 specification defining all REST endpoints
- Serves as the single source of truth for API contracts
- Must be kept in sync with implementation

## Development Structure (Expected)
Based on the tech stack, the project will likely expand to include:

```
totrackit/
├── backend/               # Java/Micronaut backend
│   ├── src/main/java/     # Java source code
│   ├── src/test/java/     # Test code
│   ├── build.gradle       # Gradle build configuration
│   └── gradlew           # Gradle wrapper
├── frontend/             # React frontend
│   ├── src/              # React components and logic
│   ├── public/           # Static assets
│   └── package.json      # Node.js dependencies
├── docker/               # Docker configurations
├── k8s/                  # Kubernetes manifests
└── docs/                 # Additional documentation
```

## API Structure
The API follows RESTful conventions with these main resource groups:
- **/admin**: Administrative operations (API keys, system management)
- **/users**: User management operations
- **/namespaces**: Multi-tenant namespace management
- **/processes**: Core process tracking functionality

## Naming Conventions
- **API Endpoints**: Use kebab-case for multi-word resources
- **JSON Fields**: Use snake_case for consistency with API specification
- **Java Classes**: Follow standard Java PascalCase conventions
- **Database Tables**: Use snake_case naming

## Configuration Management
- Environment-specific configurations should be externalized
- Use Micronaut's configuration properties system
- Support for multiple deployment environments (dev, staging, prod)

## Security Considerations
- API keys follow format: `admin_*` for admin keys, `ns_*` for namespace keys
- All endpoints require authentication via API key or OAuth2
- Namespace isolation enforced at the API level
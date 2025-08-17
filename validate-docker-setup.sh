#!/bin/bash

# Validation script for Docker Compose setup
# This script performs basic validation of the Docker configuration files

set -e

echo "🔍 Validating Docker Compose Setup"
echo "=================================="

# Check if required files exist
echo "📁 Checking required files..."

required_files=(
    "docker-compose.yml"
    "Dockerfile"
    ".dockerignore"
    "docker/postgres/init.sql"
    "src/main/resources/application-docker.yml"
)

for file in "${required_files[@]}"; do
    if [[ -f "$file" ]]; then
        echo "✅ $file exists"
    else
        echo "❌ $file is missing"
        exit 1
    fi
done

# Validate docker-compose.yml syntax
echo ""
echo "🔧 Validating docker-compose.yml syntax..."
if command -v docker-compose &> /dev/null; then
    if docker-compose config > /dev/null 2>&1; then
        echo "✅ docker-compose.yml syntax is valid"
    else
        echo "❌ docker-compose.yml syntax is invalid"
        docker-compose config
        exit 1
    fi
else
    echo "⚠️  docker-compose not available, skipping syntax validation"
fi

# Check Dockerfile syntax (basic)
echo ""
echo "🐳 Validating Dockerfile..."
if grep -q "FROM.*jdk21" Dockerfile && grep -q "EXPOSE 8080" Dockerfile; then
    echo "✅ Dockerfile appears to be correctly configured"
else
    echo "❌ Dockerfile may have issues"
    exit 1
fi

# Validate PostgreSQL init script
echo ""
echo "🗄️  Validating PostgreSQL init script..."
if grep -q "CREATE EXTENSION" docker/postgres/init.sql && grep -q "health_check" docker/postgres/init.sql; then
    echo "✅ PostgreSQL init script is properly configured"
else
    echo "❌ PostgreSQL init script may have issues"
    exit 1
fi

# Check application configuration
echo ""
echo "⚙️  Validating application configuration..."
if grep -q "jdbc:postgresql://postgres:5432" src/main/resources/application-docker.yml; then
    echo "✅ Docker application configuration is correct"
else
    echo "❌ Docker application configuration may have issues"
    exit 1
fi

echo ""
echo "🎉 All validations passed!"
echo ""
echo "Next steps:"
echo "1. Ensure Docker and Docker Compose are installed"
echo "2. Run: docker-compose up -d postgres"
echo "3. Wait for PostgreSQL to be healthy"
echo "4. Run: docker-compose up --build app"
echo ""
echo "For testing, you can also run:"
echo "  ./docker/test-environment.sh"
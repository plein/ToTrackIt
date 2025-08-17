#!/bin/bash

# Test script for Docker Compose environment
# This script validates that the Docker environment can start successfully

set -e

echo "🐳 Testing ToTrackIt Docker Environment"
echo "======================================"

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed or not in PATH"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed or not in PATH"
    exit 1
fi

echo "✅ Docker and Docker Compose are available"

# Clean up any existing containers
echo "🧹 Cleaning up existing containers..."
docker-compose down -v --remove-orphans 2>/dev/null || true

# Start PostgreSQL service
echo "🚀 Starting PostgreSQL service..."
docker-compose up -d postgres

# Wait for PostgreSQL to be healthy
echo "⏳ Waiting for PostgreSQL to be healthy..."
timeout=60
counter=0

while [ $counter -lt $timeout ]; do
    if docker-compose ps postgres | grep -q "healthy"; then
        echo "✅ PostgreSQL is healthy"
        break
    fi
    
    if [ $counter -eq $timeout ]; then
        echo "❌ PostgreSQL failed to become healthy within ${timeout} seconds"
        docker-compose logs postgres
        exit 1
    fi
    
    sleep 2
    counter=$((counter + 2))
    echo "   Waiting... (${counter}s/${timeout}s)"
done

# Test database connectivity
echo "🔍 Testing database connectivity..."
if docker-compose exec -T postgres psql -U totrackit -d totrackit -c "SELECT health_check();" > /dev/null 2>&1; then
    echo "✅ Database connectivity test passed"
else
    echo "❌ Database connectivity test failed"
    docker-compose logs postgres
    exit 1
fi

# Test database initialization
echo "🔍 Testing database initialization..."
if docker-compose exec -T postgres psql -U totrackit -d totrackit -c "SELECT 1;" > /dev/null 2>&1; then
    echo "✅ Database initialization test passed"
else
    echo "❌ Database initialization test failed"
    exit 1
fi

# Build application image (without starting)
echo "🏗️  Building application image..."
if docker-compose build app; then
    echo "✅ Application image built successfully"
else
    echo "❌ Application image build failed"
    exit 1
fi

# Clean up
echo "🧹 Cleaning up test environment..."
docker-compose down -v

echo ""
echo "🎉 All tests passed! Docker environment is ready."
echo ""
echo "To start the full environment:"
echo "  docker-compose up --build"
echo ""
echo "To start just the database:"
echo "  docker-compose up -d postgres"
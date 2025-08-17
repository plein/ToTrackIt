#!/bin/bash

# Alternative setup script for local PostgreSQL (without Docker)
# This script helps set up PostgreSQL locally on macOS

echo "🗄️  Setting up local PostgreSQL for ToTrackIt"
echo "============================================="

# Check if PostgreSQL is already installed
if command -v psql &> /dev/null; then
    echo "✅ PostgreSQL is already installed"
    
    # Check if it's running
    if pg_isready -q; then
        echo "✅ PostgreSQL is running"
    else
        echo "⚠️  PostgreSQL is installed but not running"
        echo "Try starting it with: brew services start postgresql@15"
    fi
else
    echo "❌ PostgreSQL is not installed"
    echo ""
    echo "To install PostgreSQL on macOS:"
    echo "1. Using Homebrew (recommended):"
    echo "   brew install postgresql@15"
    echo "   brew services start postgresql@15"
    echo ""
    echo "2. Using PostgreSQL.app:"
    echo "   Download from: https://postgresapp.com/"
    echo ""
    exit 1
fi

# Create database and user
echo ""
echo "🔧 Setting up ToTrackIt database..."

# Create user and database
createuser -s totrackit 2>/dev/null || echo "User 'totrackit' already exists"
createdb -O totrackit totrackit 2>/dev/null || echo "Database 'totrackit' already exists"

# Set password (you'll be prompted)
echo "Setting password for user 'totrackit'..."
psql -c "ALTER USER totrackit PASSWORD 'totrackit';" postgres

# Test connection
echo ""
echo "🔍 Testing database connection..."
if psql -U totrackit -d totrackit -c "SELECT 'Connection successful!' as status;" > /dev/null 2>&1; then
    echo "✅ Database connection successful!"
    echo ""
    echo "Database is ready at:"
    echo "  Host: localhost"
    echo "  Port: 5432"
    echo "  Database: totrackit"
    echo "  Username: totrackit"
    echo "  Password: totrackit"
    echo ""
    echo "You can now run the application with:"
    echo "  MICRONAUT_ENVIRONMENTS=local ./gradlew run"
else
    echo "❌ Database connection failed"
    echo "Please check your PostgreSQL installation"
    exit 1
fi
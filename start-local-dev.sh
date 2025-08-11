#!/bin/bash

echo "🚀 Starting E-commerce Local Development Environment"
echo "=================================================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Stop and remove existing containers
echo "🧹 Cleaning up existing containers..."
docker-compose down -v

# Start infrastructure services
echo "🐳 Starting Docker containers..."
docker-compose up -d postgres localstack adminer

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check if PostgreSQL is ready
echo "🔍 Checking PostgreSQL..."
until docker-compose exec -T postgres pg_isready -U ecommerce_user -d ecommerce; do
    echo "Waiting for PostgreSQL..."
    sleep 2
done

# Check if LocalStack is ready
echo "🔍 Checking LocalStack..."
until curl -s http://localhost:4566/_localstack/health > /dev/null; do
    echo "Waiting for LocalStack..."
    sleep 2
done

# Initialize AWS services
echo "☁️ Initializing AWS services..."
chmod +x localstack-init/01-setup-aws-services.sh
./localstack-init/01-setup-aws-services.sh

echo ""
echo "✅ Local development environment is ready!"
echo ""
echo "📊 Services Status:"
echo "- PostgreSQL: http://localhost:5432"
echo "- LocalStack: http://localhost:4566"

echo "- Adminer (DB Manager): http://localhost:8081"
echo ""
echo "🔧 Next steps:"
echo "1. Start the backend: cd backend && mvn spring-boot:run -Dspring.profiles.active=local"
echo "2. Start the frontend: cd frontend && npm run dev"
echo ""
echo "📝 Database credentials:"
echo "- Database: ecommerce"
echo "- Username: ecommerce_user"
echo "- Password: ecommerce_password"
echo ""
echo "🔑 Test users:"
echo "- Admin: admin@ecommerce.com"
echo "- User: john.doe@example.com"
echo ""
echo "🛑 To stop all services: docker-compose down" 
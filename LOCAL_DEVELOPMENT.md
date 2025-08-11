# 🏠 Local Development Setup

This guide will help you set up the complete e-commerce development environment locally.

## 📋 Prerequisites

- **Docker & Docker Compose** - For running infrastructure services
- **Java 17** - For backend development
- **Node.js 18+** - For frontend development
- **Maven** - For building backend
- **AWS CLI** - For LocalStack interaction (optional)

## 🚀 Quick Start

### 1. Start Infrastructure Services

```bash
# Make the script executable (Linux/Mac)
chmod +x start-local-dev.sh

# Run the startup script
./start-local-dev.sh
```

Or manually:

```bash
# Start all services
docker-compose up -d

# Initialize AWS services
./localstack-init/01-setup-aws-services.sh
```

### 2. Start Backend

```bash
cd backend

# Run with local profile
mvn spring-boot:run -Dspring.profiles.active=local
```

Backend will be available at: `http://localhost:8080/api`

### 3. Start Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

Frontend will be available at: `http://localhost:3000`

## 🏗️ Infrastructure Services

| Service | URL | Description |
|---------|-----|-------------|
| **PostgreSQL** | `localhost:5432` | Main database |
| **LocalStack** | `localhost:4566` | AWS services emulator |
| **Adminer** | `http://localhost:8081` | Database management UI |

## 🗄️ Database

- **Database**: `ecommerce`
- **Username**: `ecommerce_user`
- **Password**: `ecommerce_password`

### Test Data

The database is automatically populated with test data:

#### Categories
- Electronics (Smartphones, Laptops)
- Clothing (Men, Women)
- Books (Fiction, Non-Fiction)
- Home & Garden
- Sports

#### Products
- iPhone 15 Pro ($999.99)
- Samsung Galaxy S24 ($899.99)
- MacBook Pro 14" ($1999.99)
- Dell XPS 13 ($1299.99)
- Men Casual T-Shirt ($29.99)
- Women Summer Dress ($79.99)
- The Great Gatsby ($12.99)
- Clean Code ($39.99)
- Garden Tool Set ($89.99)
- Yoga Mat ($49.99)
- Wireless Headphones ($199.99)
- Smart Watch ($299.99)

#### Users
- **Admin**: `admin@ecommerce.com` (ADMIN role)
- **Regular Users**: `john.doe@example.com`, `jane.smith@example.com`, etc.

## ☁️ AWS Services (LocalStack)

### S3
- **Bucket**: `local-product-images`
- **Endpoint**: `http://localhost:4566`

### SQS
- **Queues**: `OrderStatusUpdateQueue`, `OrderStatusUpdateDLQ`
- **Endpoint**: `http://localhost:4566`

### SNS
- **Topics**: `OrderUnconfirmed`, `OrderStatusUpdated`, `PaymentCompleted`, `PaymentSuspicious`, `Exception`
- **Endpoint**: `http://localhost:4566`

### DynamoDB
- **Table**: `dev-Exceptions`
- **Endpoint**: `http://localhost:4566`

### Secrets Manager
- **Secret**: `dev/shippo` (Shippo API credentials)
- **Endpoint**: `http://localhost:4566`

## 🔧 Configuration

### Backend Configuration

The backend uses `application-local.yml` profile with:
- Local PostgreSQL connection
- LocalStack AWS endpoints
- Disabled security for development
- Debug logging enabled

### Frontend Configuration

Create `.env.local` in the frontend directory:

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_AWS_REGION=us-east-1
VITE_COGNITO_USER_POOL_ID=local-user-pool
VITE_COGNITO_CLIENT_ID=local-client-id
VITE_S3_BUCKET=local-product-images
```

## 🧪 Testing

### Backend Tests

```bash
cd backend

# Run unit tests
mvn test

# Run integration tests
mvn test -Dspring.profiles.active=test

# Run all tests
mvn verify
```

### Frontend Tests

```bash
cd frontend

# Run unit tests
npm test

# Run e2e tests
npm run test:e2e
```

## 🐛 Debugging

### Database

Access Adminer at `http://localhost:8081`:
- System: PostgreSQL
- Server: postgres
- Username: ecommerce_user
- Password: ecommerce_password
- Database: ecommerce

### LocalStack

Check LocalStack status:
```bash
curl http://localhost:4566/_localstack/health
```

List S3 buckets:
```bash
aws --endpoint-url=http://localhost:4566 s3 ls
```

List SQS queues:
```bash
aws --endpoint-url=http://localhost:4566 sqs list-queues
```

### Backend Logs

```bash
# View backend logs
docker-compose logs -f backend

# Or if running locally
tail -f backend/logs/application.log
```

## 🛑 Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (clears data)
docker-compose down -v
```

## 🔄 Reset Environment

To completely reset the environment:

```bash
# Stop and remove everything
docker-compose down -v

# Remove all images
docker system prune -a

# Restart
./start-local-dev.sh
```

## 📚 API Documentation

Once the backend is running, you can access:

- **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`
- **API Docs**: `http://localhost:8080/api/v3/api-docs`

## 🚨 Troubleshooting

### Common Issues

1. **Port conflicts**: Make sure ports 5432, 4566, 6379, 8080, 8081 are available
2. **Docker not running**: Ensure Docker Desktop is started
3. **Database connection**: Wait for PostgreSQL to fully start (check logs)
4. **LocalStack issues**: Restart LocalStack container if AWS services fail

### Logs

```bash
# View all logs
docker-compose logs

# View specific service logs
docker-compose logs postgres
docker-compose logs localstack
```

### Reset Database

```bash
# Remove database volume
docker-compose down -v

# Restart services
docker-compose up -d postgres

# Re-run initialization
./init-db/01-init.sql
```

## 📞 Support

If you encounter issues:

1. Check the logs: `docker-compose logs`
2. Verify all services are running: `docker-compose ps`
3. Check network connectivity: `docker network ls`
4. Restart the environment: `./start-local-dev.sh` 
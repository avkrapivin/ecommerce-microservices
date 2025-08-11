# PowerShell script for starting local development environment on Windows

Write-Host "🚀 Starting E-commerce Local Development Environment" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green

# Check if Docker is running
try {
    docker info | Out-Null
} catch {
    Write-Host "❌ Docker is not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

# Stop and remove existing containers
Write-Host "🧹 Cleaning up existing containers..." -ForegroundColor Yellow
docker-compose down -v

# Start infrastructure services
Write-Host "🐳 Starting Docker containers..." -ForegroundColor Yellow
docker-compose up -d postgres localstack adminer

# Wait for services to be ready
Write-Host "⏳ Waiting for services to be ready..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# Check if PostgreSQL is ready
Write-Host "🔍 Checking PostgreSQL..." -ForegroundColor Yellow
do {
    try {
        docker-compose exec -T postgres pg_isready -U ecommerce_user -d ecommerce | Out-Null
        break
    } catch {
        Write-Host "Waiting for PostgreSQL..." -ForegroundColor Gray
        Start-Sleep -Seconds 2
    }
} while ($true)

# Check if LocalStack is ready
Write-Host "🔍 Checking LocalStack..." -ForegroundColor Yellow
do {
    try {
        Invoke-RestMethod -Uri "http://localhost:4566/_localstack/health" | Out-Null
        break
    } catch {
        Write-Host "Waiting for LocalStack..." -ForegroundColor Gray
        Start-Sleep -Seconds 2
    }
} while ($true)

# Initialize AWS services
Write-Host "☁️ Initializing AWS services..." -ForegroundColor Yellow
try {
    # Set environment variables for AWS CLI
    $env:AWS_ACCESS_KEY_ID = "test"
    $env:AWS_SECRET_ACCESS_KEY = "test"
    $env:AWS_DEFAULT_REGION = "us-east-1"
    $env:AWS_ENDPOINT_URL = "http://localhost:4566"

    # Create S3 bucket
    aws --endpoint-url=http://localhost:4566 s3 mb s3://local-product-images
    aws --endpoint-url=http://localhost:4566 s3api put-bucket-acl --bucket local-product-images --acl public-read

    # Create SQS queues
    aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name OrderStatusUpdateQueue
    aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name OrderStatusUpdateDLQ

    # Create SNS topics
    aws --endpoint-url=http://localhost:4566 sns create-topic --name OrderUnconfirmed
    aws --endpoint-url=http://localhost:4566 sns create-topic --name OrderStatusUpdated
    aws --endpoint-url=http://localhost:4566 sns create-topic --name PaymentCompleted
    aws --endpoint-url=http://localhost:4566 sns create-topic --name PaymentSuspicious
    aws --endpoint-url=http://localhost:4566 sns create-topic --name Exception

    # Create DynamoDB table
    aws --endpoint-url=http://localhost:4566 dynamodb create-table `
        --table-name dev-Exceptions `
        --attribute-definitions AttributeName=partitionKey,AttributeType=S AttributeName=timestamp,AttributeType=S `
        --key-schema AttributeName=partitionKey,KeyType=HASH AttributeName=timestamp,KeyType=RANGE `
        --billing-mode PAY_PER_REQUEST

    # Create Secrets Manager secret
    aws --endpoint-url=http://localhost:4566 secretsmanager create-secret `
        --name "dev/shippo" `
        --description "Shippo API credentials" `
        --secret-string '{"apiKey":"test-shippo-key","fromName":"Test Store","fromStreet":"123 Test St","fromCity":"Test City","fromState":"TS","fromZip":"12345","fromCountry":"US","fromPhone":"+1234567890"}'

} catch {
    Write-Host "⚠️ Warning: Some AWS services may not have been initialized properly. Check if AWS CLI is installed." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "✅ Local development environment is ready!" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Services Status:" -ForegroundColor Cyan
Write-Host "- PostgreSQL: localhost:5432" -ForegroundColor White
Write-Host "- LocalStack: http://localhost:4566" -ForegroundColor White

Write-Host "- Adminer (DB Manager): http://localhost:8081" -ForegroundColor White
Write-Host ""
Write-Host "🔧 Next steps:" -ForegroundColor Cyan
Write-Host "1. Start the backend: cd backend && mvn spring-boot:run -Dspring.profiles.active=local" -ForegroundColor White
Write-Host "2. Start the frontend: cd frontend && npm run dev" -ForegroundColor White
Write-Host ""
Write-Host "📝 Database credentials:" -ForegroundColor Cyan
Write-Host "- Database: ecommerce" -ForegroundColor White
Write-Host "- Username: postgres" -ForegroundColor White
Write-Host "- Password: postgres_password" -ForegroundColor White
Write-Host ""
Write-Host "🔑 Test users:" -ForegroundColor Cyan
Write-Host "- Admin: admin@ecommerce.com" -ForegroundColor White
Write-Host "- User: john.doe@example.com" -ForegroundColor White
Write-Host ""
Write-Host "🛑 To stop all services: docker-compose down" -ForegroundColor Yellow 
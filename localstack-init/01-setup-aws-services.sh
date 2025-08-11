#!/bin/bash

# Wait for LocalStack to be ready
echo "Waiting for LocalStack to be ready..."
while ! curl -s http://localhost:4566/_localstack/health > /dev/null; do
    sleep 2
done
echo "LocalStack is ready!"

# Set AWS CLI to use LocalStack
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
export AWS_ENDPOINT_URL=http://localhost:4566

# Create S3 bucket for product images
echo "Creating S3 bucket for product images..."
aws --endpoint-url=http://localhost:4566 s3 mb s3://local-product-images
aws --endpoint-url=http://localhost:4566 s3api put-bucket-acl --bucket local-product-images --acl public-read

# Create SQS queues
echo "Creating SQS queues..."
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name OrderStatusUpdateQueue
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name OrderStatusUpdateDLQ

# Create SNS topics
echo "Creating SNS topics..."
aws --endpoint-url=http://localhost:4566 sns create-topic --name OrderUnconfirmed
aws --endpoint-url=http://localhost:4566 sns create-topic --name OrderStatusUpdated
aws --endpoint-url=http://localhost:4566 sns create-topic --name PaymentCompleted
aws --endpoint-url=http://localhost:4566 sns create-topic --name PaymentSuspicious
aws --endpoint-url=http://localhost:4566 sns create-topic --name Exception

# Subscribe SQS queue to SNS topic
echo "Subscribing SQS to SNS..."
QUEUE_URL=$(aws --endpoint-url=http://localhost:4566 sqs get-queue-url --queue-name OrderStatusUpdateQueue --query 'QueueUrl' --output text)
TOPIC_ARN=$(aws --endpoint-url=http://localhost:4566 sns list-topics --query 'Topics[?contains(TopicArn, `OrderStatusUpdated`)].TopicArn' --output text)

aws --endpoint-url=http://localhost:4566 sns subscribe \
    --topic-arn "$TOPIC_ARN" \
    --protocol sqs \
    --notification-endpoint "$QUEUE_URL" \
    --attributes '{"RawMessageDelivery": "true"}'

# Create Cognito User Pool (simulated)
echo "Setting up Cognito User Pool..."
# Note: LocalStack Cognito support is limited, we'll use mock values
echo "Cognito User Pool ID: local-user-pool"
echo "Cognito Client ID: local-client-id"

# Create Secrets Manager secrets
echo "Creating Secrets Manager secrets..."
aws --endpoint-url=http://localhost:4566 secretsmanager create-secret \
    --name "dev/shippo" \
    --description "Shippo API credentials" \
    --secret-string '{"apiKey":"test-shippo-key","fromName":"Test Store","fromStreet":"123 Test St","fromCity":"Test City","fromState":"TS","fromZip":"12345","fromCountry":"US","fromPhone":"+1234567890"}'

# Create DynamoDB table for exceptions
echo "Creating DynamoDB table for exceptions..."
aws --endpoint-url=http://localhost:4566 dynamodb create-table \
    --table-name dev-Exceptions \
    --attribute-definitions AttributeName=partitionKey,AttributeType=S AttributeName=timestamp,AttributeType=S \
    --key-schema AttributeName=partitionKey,KeyType=HASH AttributeName=timestamp,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST

echo "AWS services setup completed!"
echo ""
echo "Available services:"
echo "- S3 Bucket: local-product-images"
echo "- SQS Queues: OrderStatusUpdateQueue, OrderStatusUpdateDLQ"
echo "- SNS Topics: OrderUnconfirmed, OrderStatusUpdated, PaymentCompleted, PaymentSuspicious, Exception"
echo "- DynamoDB Table: dev-Exceptions"
echo "- Secrets Manager: dev/shippo"
echo ""
echo "LocalStack endpoint: http://localhost:4566" 
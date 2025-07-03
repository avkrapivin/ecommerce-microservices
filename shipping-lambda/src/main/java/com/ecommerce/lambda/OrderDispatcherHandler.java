package com.ecommerce.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.ecommerce.lambda.model.Order;
import com.ecommerce.lambda.service.AwsSnsPublisher;
import com.ecommerce.lambda.service.OrderDispatcherService;
import com.ecommerce.lambda.service.PayPalService;
import com.ecommerce.lambda.service.PayPalClientImpl;
import com.ecommerce.lambda.service.SnsPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.SnsClient;

@Slf4j
public class OrderDispatcherHandler implements RequestHandler<SNSEvent, Void> {
    private final OrderDispatcherService orderDispatcherService;
    private final ObjectMapper objectMapper;

    public OrderDispatcherHandler() {
        SnsClient snsClient = SnsClient.builder().build();
        SnsPublisher snsPublisher = new AwsSnsPublisher(snsClient);
        
        String paymentCompletedTopicArn = System.getenv("PAYMENT_COMPLETED_TOPIC_ARN");
        String paymentSuspiciousTopicArn = System.getenv("PAYMENT_SUSPICIOUS_TOPIC_ARN");
        String orderStatusUpdatedTopicArn = System.getenv("ORDER_STATUS_UPDATED_TOPIC_ARN");
        
        // Создаем PayPalClient с реальными данными
        PayPalClientImpl payPalClient = new PayPalClientImpl(
            System.getenv("PAYPAL_CLIENT_ID"),
            System.getenv("PAYPAL_CLIENT_SECRET"),
            System.getenv("PAYPAL_MODE")
        );
        
        PayPalService payPalService = new PayPalService(payPalClient);

        this.orderDispatcherService = new OrderDispatcherService(
            snsPublisher,
            payPalService,
            paymentCompletedTopicArn,
            paymentSuspiciousTopicArn,
            orderStatusUpdatedTopicArn
        );
        this.objectMapper = new ObjectMapper();
    }

    // Constructor for testing
    public OrderDispatcherHandler(OrderDispatcherService orderDispatcherService, ObjectMapper objectMapper) {
        this.orderDispatcherService = orderDispatcherService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Void handleRequest(SNSEvent event, Context context) {
        try {
            for (SNSEvent.SNSRecord record : event.getRecords()) {
                String message = record.getSNS().getMessage();
                Order order = objectMapper.readValue(message, Order.class);
                orderDispatcherService.processOrder(order);
            }
            return null;
        } catch (Exception e) {
            log.error("Error processing order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process order", e);
        }
    }
} 
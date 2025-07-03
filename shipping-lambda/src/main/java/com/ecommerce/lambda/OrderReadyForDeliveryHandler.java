package com.ecommerce.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.ecommerce.lambda.model.PaymentCompletedEvent;
import com.ecommerce.lambda.service.AwsSnsPublisher;
import com.ecommerce.lambda.service.OrderDeliveryService;
import com.ecommerce.lambda.service.SnsPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.SnsClient;

@Slf4j
public class OrderReadyForDeliveryHandler implements RequestHandler<SNSEvent, Void> {
    private final OrderDeliveryService orderDeliveryService;
    private final ObjectMapper objectMapper;

    public OrderReadyForDeliveryHandler() {
        // Инициализация SNS Publisher
        SnsClient snsClient = SnsClient.builder().build();
        SnsPublisher snsPublisher = new AwsSnsPublisher(snsClient);
        
        // Получаем ARN топика из переменных окружения
        String orderReadyForDeliveryTopicArn = System.getenv("ORDER_READY_FOR_DELIVERY_TOPIC_ARN");
        
        this.orderDeliveryService = new OrderDeliveryService(snsPublisher, orderReadyForDeliveryTopicArn);
        this.objectMapper = new ObjectMapper();
    }

    // Constructor for testing
    public OrderReadyForDeliveryHandler(OrderDeliveryService orderDeliveryService, ObjectMapper objectMapper) {
        this.orderDeliveryService = orderDeliveryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Void handleRequest(SNSEvent event, Context context) {
        try {
            log.info("Received SNS event: {}", event);

            for (SNSEvent.SNSRecord record : event.getRecords()) {
                String message = record.getSNS().getMessage();
                log.info("Processing message: {}", message);

                PaymentCompletedEvent paymentEvent = objectMapper.readValue(message, PaymentCompletedEvent.class);
                orderDeliveryService.prepareOrderForDelivery(paymentEvent);
            }

            return null;
        } catch (Exception e) {
            log.error("Error processing SNS event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process SNS event", e);
        }
    }
} 
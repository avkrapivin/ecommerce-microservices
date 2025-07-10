package com.ecommerce.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.ecommerce.lambda.model.OrderReadyForDeliveryEvent;
import com.ecommerce.lambda.model.OrderStatusUpdateEvent;
import com.ecommerce.lambda.model.PaymentCompletedEvent;
import com.ecommerce.lambda.service.NotificationService;
import com.ecommerce.lambda.service.SimpleEmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * Lambda функция для обработки уведомлений по email
 */
@Slf4j
public class NotificationHandler implements RequestHandler<SNSEvent, Void> {
    
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    
    public NotificationHandler() {
        // Инициализация SES клиента
        SesV2Client sesClient = SesV2Client.builder().build();
        
        // Получаем email отправителя из переменных окружения
        String fromEmail = System.getenv("FROM_EMAIL");
        if (fromEmail == null || fromEmail.trim().isEmpty()) {
            throw new IllegalStateException("FROM_EMAIL environment variable is required");
        }
        
        // Создаем email сервис
        SimpleEmailService emailService = new SimpleEmailService(sesClient, fromEmail);
        
        // Создаем notification сервис
        this.notificationService = new NotificationService(emailService);
        this.objectMapper = new ObjectMapper();
        
        log.info("NotificationHandler initialized with fromEmail: {}", fromEmail);
    }
    
    // Конструктор для тестирования
    public NotificationHandler(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Void handleRequest(SNSEvent event, Context context) {
        try {
            log.info("Received SNS notification event with {} records", event.getRecords().size());
            
            for (SNSEvent.SNSRecord record : event.getRecords()) {
                processRecord(record);
            }
            
            log.info("Successfully processed all notification records");
            return null;
            
        } catch (Exception e) {
            log.error("Error processing SNS notification event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process SNS notification event", e);
        }
    }
    
    private void processRecord(SNSEvent.SNSRecord record) {
        try {
            String topicArn = record.getSNS().getTopicArn();
            String message = record.getSNS().getMessage();
            String messageId = record.getSNS().getMessageId();
            
            log.info("Processing notification record {} from topic: {}", messageId, topicArn);
            log.debug("Message content: {}", message);
            
            // Определяем тип события по ARN топика
            if (topicArn.contains("PaymentCompleted")) {
                PaymentCompletedEvent paymentEvent = objectMapper.readValue(message, PaymentCompletedEvent.class);
                notificationService.processPaymentCompleted(paymentEvent);
                
            } else if (topicArn.contains("order-ready-for-delivery")) {
                OrderReadyForDeliveryEvent deliveryEvent = objectMapper.readValue(message, OrderReadyForDeliveryEvent.class);
                notificationService.processOrderReadyForDelivery(deliveryEvent);
                
            } else if (topicArn.contains("OrderStatusUpdated")) {
                OrderStatusUpdateEvent statusEvent = objectMapper.readValue(message, OrderStatusUpdateEvent.class);
                notificationService.processOrderStatusUpdated(statusEvent);
                
            } else {
                log.warn("Unknown topic ARN: {}. Skipping notification processing.", topicArn);
                return;
            }
            
            log.info("Successfully processed notification record {}", messageId);
            
        } catch (Exception e) {
            log.error("Error processing notification record: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process notification record", e);
        }
    }
} 
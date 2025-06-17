package com.ecommerce.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.ecommerce.lambda.model.OrderReadyForDeliveryEvent;
import com.ecommerce.lambda.service.ShippoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.SnsClient;

@Slf4j
public class ProcessDeliveryHandler implements RequestHandler<SNSEvent, Void> {
    private final ShippoService shippoService;
    private final ObjectMapper objectMapper;

    public ProcessDeliveryHandler() {
        // Инициализация клиентов AWS
        SnsClient snsClient = SnsClient.builder().build();
        
        // Получаем ARN топика и API ключ из переменных окружения
        String orderStatusUpdatedTopicArn = System.getenv("ORDER_STATUS_UPDATED_TOPIC_ARN");
        String shippoApiKey = System.getenv("SHIPPO_API_KEY");
        
        this.shippoService = new ShippoService(snsClient, orderStatusUpdatedTopicArn, shippoApiKey);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Void handleRequest(SNSEvent event, Context context) {
        try {
            log.info("Received SNS event: {}", event);

            for (SNSEvent.SNSRecord record : event.getRecords()) {
                String messageId = record.getSNS().getMessageId();
                String message = record.getSNS().getMessage();
                log.info("Processing message {}: {}", messageId, message);

                OrderReadyForDeliveryEvent deliveryEvent = objectMapper.readValue(message, OrderReadyForDeliveryEvent.class);
                shippoService.processDelivery(deliveryEvent);
                
                log.info("Successfully processed message {}", messageId);
            }

            return null;
        } catch (Exception e) {
            log.error("Error processing SNS event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process SNS event", e);
        }
    }
} 
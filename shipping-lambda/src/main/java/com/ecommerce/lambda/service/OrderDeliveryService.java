package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.Order;
import com.ecommerce.lambda.model.PaymentCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Slf4j
public class OrderDeliveryService {
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String orderReadyForDeliveryTopicArn;

    public OrderDeliveryService(SnsClient snsClient, String orderReadyForDeliveryTopicArn) {
        this.snsClient = snsClient;
        this.objectMapper = new ObjectMapper();
        this.orderReadyForDeliveryTopicArn = orderReadyForDeliveryTopicArn;
    }

    public void prepareOrderForDelivery(PaymentCompletedEvent event) {
        try {
            log.info("Preparing order {} for delivery", event.getOrderNumber());

            // Здесь можно добавить дополнительную логику подготовки заказа
            // Например, проверка наличия товаров, формирование упаковочного листа и т.д.

            // Публикуем событие о готовности заказа к доставке
            String message = objectMapper.writeValueAsString(event);
            PublishRequest request = PublishRequest.builder()
                    .topicArn(orderReadyForDeliveryTopicArn)
                    .message(message)
                    .subject("OrderReadyForDelivery")
                    .build();

            snsClient.publish(request);
            log.info("Order {} is ready for delivery", event.getOrderNumber());
        } catch (Exception e) {
            log.error("Error preparing order {} for delivery: {}", event.getOrderNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to prepare order for delivery", e);
        }
    }
} 
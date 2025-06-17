package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.Order;
import com.ecommerce.lambda.model.PaymentCompletedEvent;
import com.ecommerce.lambda.model.OrderReadyForDeliveryEvent;
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

            // Создаем событие OrderReadyForDelivery
            OrderReadyForDeliveryEvent deliveryEvent = new OrderReadyForDeliveryEvent();
            
            // Копируем все поля из PaymentCompletedEvent
            deliveryEvent.setOrderId(event.getOrderId());
            deliveryEvent.setOrderNumber(event.getOrderNumber());
            deliveryEvent.setCustomerEmail(event.getCustomerEmail());
            deliveryEvent.setCustomerName(event.getCustomerName());
            deliveryEvent.setShippingAddress(event.getShippingAddress());
            deliveryEvent.setShippingCity(event.getShippingCity());
            deliveryEvent.setShippingState(event.getShippingState());
            deliveryEvent.setShippingZip(event.getShippingZip());
            deliveryEvent.setShippingCountry(event.getShippingCountry());
            deliveryEvent.setTotalAmount(event.getTotalAmount());
            deliveryEvent.setCurrency(event.getCurrency());
            
            // Копируем параметры посылки
            deliveryEvent.setParcelLength(event.getParcelLength());
            deliveryEvent.setParcelWidth(event.getParcelWidth());
            deliveryEvent.setParcelHeight(event.getParcelHeight());
            deliveryEvent.setParcelDistanceUnit(event.getParcelDistanceUnit());
            deliveryEvent.setParcelWeight(event.getParcelWeight());
            deliveryEvent.setParcelMassUnit(event.getParcelMassUnit());

            // Публикуем событие о готовности заказа к доставке
            String message = objectMapper.writeValueAsString(deliveryEvent);
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
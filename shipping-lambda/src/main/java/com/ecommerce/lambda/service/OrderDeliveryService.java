package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.Order;
import com.ecommerce.lambda.model.PaymentCompletedEvent;
import com.ecommerce.lambda.model.OrderReadyForDeliveryEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderDeliveryService {
    private final SnsPublisher snsPublisher;
    private final ObjectMapper objectMapper;
    private final String orderReadyForDeliveryTopicArn;

    public OrderDeliveryService(SnsPublisher snsPublisher, String orderReadyForDeliveryTopicArn) {
        this.snsPublisher = snsPublisher;
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
            deliveryEvent.setParcelLength(event.getParcelLength() != null ? event.getParcelLength() : 0.0);
            deliveryEvent.setParcelWidth(event.getParcelWidth() != null ? event.getParcelWidth() : 0.0);
            deliveryEvent.setParcelHeight(event.getParcelHeight() != null ? event.getParcelHeight() : 0.0);
            deliveryEvent.setParcelDistanceUnit(event.getParcelDistanceUnit() != null ? event.getParcelDistanceUnit() : "cm");
            deliveryEvent.setParcelWeight(event.getParcelWeight() != null ? event.getParcelWeight() : 0.0);
            deliveryEvent.setParcelMassUnit(event.getParcelMassUnit() != null ? event.getParcelMassUnit() : "kg");

            // Публикуем событие о готовности заказа к доставке
            String message = objectMapper.writeValueAsString(deliveryEvent);
            snsPublisher.publishMessage(orderReadyForDeliveryTopicArn, message);
            log.info("Order {} is ready for delivery", event.getOrderNumber());
        } catch (Exception e) {
            log.error("Error preparing order {} for delivery: {}", event.getOrderNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to prepare order for delivery", e);
        }
    }
} 
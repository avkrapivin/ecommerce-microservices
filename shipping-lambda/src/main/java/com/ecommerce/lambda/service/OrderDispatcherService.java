package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.Order;
import com.ecommerce.lambda.model.Order.OrderItem;
import com.ecommerce.lambda.model.PaymentCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.api.payments.Payment;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.io.IOException;

@Slf4j
public class OrderDispatcherService {
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final PayPalService payPalService;
    private final String paymentCompletedTopicArn;
    private final String paymentSuspiciousTopicArn;
    private final String orderStatusUpdatedTopicArn;

    public OrderDispatcherService(SnsClient snsClient,
                                PayPalService payPalService,
                                String paymentCompletedTopicArn,
                                String paymentSuspiciousTopicArn,
                                String orderStatusUpdatedTopicArn) {
        this.snsClient = snsClient;
        this.objectMapper = new ObjectMapper();
        this.payPalService = payPalService;
        this.paymentCompletedTopicArn = paymentCompletedTopicArn;
        this.paymentSuspiciousTopicArn = paymentSuspiciousTopicArn;
        this.orderStatusUpdatedTopicArn = orderStatusUpdatedTopicArn;
    }

    public void processOrder(Order order) {
        try {
            String orderJson = objectMapper.writeValueAsString(order);
            
            if (isSuspiciousOrder(order)) {
                publishToSns(paymentSuspiciousTopicArn, orderJson);
                publishOrderStatusUpdate(orderJson, "PAYMENT_SUSPICIOUS");
                log.info("Order {} marked as suspicious", order.getId());
            } else {
                // Создаем заказ в PayPal
                Payment payment = payPalService.createOrder(
                    order.getTotal().doubleValue(),
                    "USD",
                    order.getOrderNumber()
                );

                // Захватываем платеж
                Payment capturedPayment = payPalService.captureOrder(payment.getId(), payment.getPayer().getPayerInfo().getPayerId());

                if (payPalService.isOrderApproved(capturedPayment)) {
                    // Создаем событие PaymentCompleted с данными для доставки
                    PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent();
                    paymentEvent.setOrderId(order.getId());
                    paymentEvent.setOrderNumber(order.getOrderNumber());
                    paymentEvent.setPaymentId(capturedPayment.getId());
                    paymentEvent.setPaymentStatus(capturedPayment.getState());
                    paymentEvent.setPaymentMethod("PAYPAL");
                    paymentEvent.setPaymentDate(capturedPayment.getCreateTime());
                    
                    // Копируем данные для доставки
                    paymentEvent.setCustomerEmail(order.getUserId());
                    paymentEvent.setCustomerName(order.getShippingAddress().getStreet());
                    paymentEvent.setShippingAddress(order.getShippingAddress().getStreet());
                    paymentEvent.setShippingCity(order.getShippingAddress().getCity());
                    paymentEvent.setShippingState(order.getShippingAddress().getState());
                    paymentEvent.setShippingZip(order.getShippingAddress().getZipCode());
                    paymentEvent.setShippingCountry(order.getShippingAddress().getCountry());
                    paymentEvent.setTotalAmount(order.getTotal().doubleValue());
                    paymentEvent.setCurrency("USD");
                    
                    // Рассчитываем параметры посылки
                    calculateParcelDimensions(order, paymentEvent);
                    
                    // Публикуем событие
                    String paymentEventJson = objectMapper.writeValueAsString(paymentEvent);
                    publishToSns(paymentCompletedTopicArn, paymentEventJson);
                    publishOrderStatusUpdate(orderJson, "PAYMENT_COMPLETED");
                    log.info("Order {} payment completed", order.getId());
                } else {
                    publishToSns(paymentSuspiciousTopicArn, orderJson);
                    publishOrderStatusUpdate(orderJson, "PAYMENT_FAILED");
                    log.warn("Order {} payment failed", order.getId());
                }
            }
        } catch (Exception e) {
            log.error("Error processing order {}: {}", order.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process order", e);
        }
    }

    private boolean isSuspiciousOrder(Order order) {
        // Здесь можно добавить логику проверки подозрительности заказа
        // Например, проверка суммы, количества товаров, истории пользователя и т.д.
        return false;
    }

    private void publishToSns(String topicArn, String message) {
        PublishRequest request = PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .build();
        snsClient.publish(request);
    }

    private void publishOrderStatusUpdate(String orderJson, String status) {
        try {
            String message = String.format("{\"order\":%s,\"status\":\"%s\"}", orderJson, status);
            publishToSns(orderStatusUpdatedTopicArn, message);
        } catch (Exception e) {
            log.error("Failed to publish order status update: {}", e.getMessage(), e);
        }
    }

    private void calculateParcelDimensions(Order order, PaymentCompletedEvent event) {
        // Простая логика расчета размеров посылки
        // В реальном приложении здесь должна быть более сложная логика
        double totalVolume = 0;
        double totalWeight = 0;
        
        for (Order.OrderItem item : order.getItems()) {
            // Предполагаем, что каждый товар имеет стандартные размеры
            double itemVolume = 10 * 10 * 5; // 10x10x5 см
            double itemWeight = 1.0; // 1 кг
            
            totalVolume += itemVolume * item.getQuantity();
            totalWeight += itemWeight * item.getQuantity();
        }
        
        // Рассчитываем размеры коробки
        double side = Math.cbrt(totalVolume);
        event.setParcelLength(side);
        event.setParcelWidth(side);
        event.setParcelHeight(side);
        event.setParcelDistanceUnit("cm");
        event.setParcelWeight(totalWeight);
        event.setParcelMassUnit("kg");
    }
} 
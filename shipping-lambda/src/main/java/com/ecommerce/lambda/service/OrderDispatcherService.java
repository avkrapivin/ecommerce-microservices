package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.Order;
import com.ecommerce.lambda.model.Order.OrderItem;
import com.ecommerce.lambda.model.PaymentCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.api.payments.Payment;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class OrderDispatcherService {
    private final SnsPublisher snsPublisher;
    private final ObjectMapper objectMapper;
    private final PayPalService payPalService;
    private final String paymentCompletedTopicArn;
    private final String paymentSuspiciousTopicArn;
    private final String orderStatusUpdatedTopicArn;

    public OrderDispatcherService(SnsPublisher snsPublisher,
                                PayPalService payPalService,
                                String paymentCompletedTopicArn,
                                String paymentSuspiciousTopicArn,
                                String orderStatusUpdatedTopicArn) {
        this.snsPublisher = snsPublisher;
        this.objectMapper = new ObjectMapper();
        this.payPalService = payPalService;
        this.paymentCompletedTopicArn = paymentCompletedTopicArn;
        this.paymentSuspiciousTopicArn = paymentSuspiciousTopicArn;
        this.orderStatusUpdatedTopicArn = orderStatusUpdatedTopicArn;
    }

    public void processOrder(Order order) {
        try {
            // Проверяем обязательные поля
            if (order == null) {
                throw new IllegalArgumentException("Order cannot be null");
            }
            if (order.getTotal() == null) {
                throw new IllegalArgumentException("Order total cannot be null");
            }
            if (order.getOrderNumber() == null) {
                throw new IllegalArgumentException("Order number cannot be null");
            }
            
            String orderJson = objectMapper.writeValueAsString(order);
            
            if (isSuspiciousOrder(order)) {
                snsPublisher.publishMessage(paymentSuspiciousTopicArn, orderJson);
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
                    
                    // Копируем данные для доставки с проверкой на null
                    paymentEvent.setCustomerEmail(order.getUserId());
                    
                    if (order.getShippingAddress() != null) {
                        paymentEvent.setCustomerName(order.getShippingAddress().getStreet());
                        paymentEvent.setShippingAddress(order.getShippingAddress().getStreet());
                        paymentEvent.setShippingCity(order.getShippingAddress().getCity());
                        paymentEvent.setShippingState(order.getShippingAddress().getState());
                        paymentEvent.setShippingZip(order.getShippingAddress().getZipCode());
                        paymentEvent.setShippingCountry(order.getShippingAddress().getCountry());
                    } else {
                        // Устанавливаем значения по умолчанию, если адрес отсутствует
                        paymentEvent.setCustomerName("Unknown");
                        paymentEvent.setShippingAddress("No address provided");
                        paymentEvent.setShippingCity("Unknown");
                        paymentEvent.setShippingState("Unknown");
                        paymentEvent.setShippingZip("00000");
                        paymentEvent.setShippingCountry("Unknown");
                    }
                    
                    paymentEvent.setTotalAmount(order.getTotal().doubleValue());
                    paymentEvent.setCurrency("USD");
                    
                    // Рассчитываем параметры посылки
                    calculateParcelDimensions(order, paymentEvent);
                    
                    // Публикуем событие
                    String paymentEventJson = objectMapper.writeValueAsString(paymentEvent);
                    snsPublisher.publishMessage(paymentCompletedTopicArn, paymentEventJson);
                    publishOrderStatusUpdate(orderJson, "PAYMENT_COMPLETED");
                    log.info("Order {} payment completed", order.getId());
                } else {
                    snsPublisher.publishMessage(paymentSuspiciousTopicArn, orderJson);
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

    private void publishOrderStatusUpdate(String orderJson, String status) {
        try {
            String message = String.format("{\"order\":%s,\"status\":\"%s\"}", orderJson, status);
            snsPublisher.publishMessage(orderStatusUpdatedTopicArn, message);
        } catch (Exception e) {
            log.error("Failed to publish order status update: {}", e.getMessage(), e);
        }
    }

    private void calculateParcelDimensions(Order order, PaymentCompletedEvent event) {
        // Простая логика расчета размеров посылки
        // В реальном приложении здесь должна быть более сложная логика
        double totalVolume = 0;
        double totalWeight = 0;
        
        if (order.getItems() != null) {
            for (Order.OrderItem item : order.getItems()) {
                // Предполагаем, что каждый товар имеет стандартные размеры
                double itemVolume = 10 * 10 * 5; // 10x10x5 см
                double itemWeight = 1.0; // 1 кг
                
                totalVolume += itemVolume * item.getQuantity();
                totalWeight += itemWeight * item.getQuantity();
            }
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
package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.OrderReadyForDeliveryEvent;
import com.ecommerce.lambda.model.OrderStatusUpdateEvent;
import com.ecommerce.lambda.model.PaymentCompletedEvent;

/**
 * Интерфейс для отправки email уведомлений
 */
public interface EmailService {
    
    /**
     * Отправляет уведомление об успешной оплате заказа
     */
    void sendPaymentCompletedEmail(PaymentCompletedEvent event);
    
    /**
     * Отправляет уведомление о готовности заказа к отправке
     */
    void sendOrderReadyForDeliveryEmail(OrderReadyForDeliveryEvent event);
    
    /**
     * Отправляет уведомление об обновлении статуса заказа
     */
    void sendOrderStatusUpdatedEmail(OrderStatusUpdateEvent event);
} 
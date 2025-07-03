package com.ecommerce.lambda.service;

import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;

/**
 * Интерфейс для работы с PayPal API
 */
public interface PayPalClient {
    
    /**
     * Создает заказ в PayPal
     * @param amount сумма заказа
     * @param currency валюта
     * @param orderNumber номер заказа
     * @return объект Payment
     * @throws PayPalRESTException при ошибке PayPal API
     */
    Payment createOrder(Double amount, String currency, String orderNumber) throws PayPalRESTException;
    
    /**
     * Захватывает платеж в PayPal
     * @param paymentId ID платежа
     * @param payerId ID плательщика
     * @return объект Payment
     * @throws PayPalRESTException при ошибке PayPal API
     */
    Payment captureOrder(String paymentId, String payerId) throws PayPalRESTException;
    
    /**
     * Проверяет, одобрен ли заказ
     * @param payment объект Payment
     * @return true если заказ одобрен
     */
    boolean isOrderApproved(Payment payment);
} 
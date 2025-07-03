package com.ecommerce.lambda.service;

import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис для работы с PayPal, использующий PayPalClient
 */
@Slf4j
public class PayPalService {
    private final PayPalClient payPalClient;

    public PayPalService(PayPalClient payPalClient) {
        this.payPalClient = payPalClient;
    }

    public Payment createOrder(double amount, String currency, String orderNumber) throws PayPalRESTException {
        return payPalClient.createOrder(amount, currency, orderNumber);
    }

    public Payment captureOrder(String paymentId, String payerId) throws PayPalRESTException {
        return payPalClient.captureOrder(paymentId, payerId);
    }

    public boolean isOrderApproved(Payment payment) {
        return payPalClient.isOrderApproved(payment);
    }
} 
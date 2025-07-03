package com.ecommerce.lambda.service;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Реализация PayPalClient, использующая PayPal SDK
 */
@Slf4j
public class PayPalClientImpl implements PayPalClient {
    
    private final APIContext apiContext;
    
    public PayPalClientImpl(String clientId, String clientSecret, String mode) {
        this.apiContext = new APIContext(clientId, clientSecret, mode);
    }
    
    @Override
    public Payment createOrder(Double amount, String currency, String orderNumber) throws PayPalRESTException {
        try {
            log.debug("Creating PayPal order for amount: {} {}, orderNumber: {}", amount, currency, orderNumber);
            
            // Создаем детали транзакции
            Details details = new Details();
            details.setSubtotal(amount.toString());
            
            // Создаем сумму
            Amount amountObj = new Amount();
            amountObj.setCurrency(currency);
            amountObj.setTotal(amount.toString());
            amountObj.setDetails(details);
            
            // Создаем транзакцию
            Transaction transaction = new Transaction();
            transaction.setAmount(amountObj);
            transaction.setDescription("Order: " + orderNumber);
            
            // Создаем список транзакций
            List<Transaction> transactions = new ArrayList<>();
            transactions.add(transaction);
            
            // Создаем платеж
            Payment payment = new Payment();
            payment.setIntent("authorize");
            payment.setPayer(new Payer());
            payment.setTransactions(transactions);
            
            // Создаем заказ в PayPal
            Payment createdPayment = payment.create(apiContext);
            log.info("PayPal order created successfully: {}", createdPayment.getId());
            
            return createdPayment;
        } catch (PayPalRESTException e) {
            log.error("Failed to create PayPal order: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public Payment captureOrder(String paymentId, String payerId) throws PayPalRESTException {
        try {
            log.debug("Capturing PayPal payment: {}, payerId: {}", paymentId, payerId);
            
            // Создаем объект для захвата платежа
            PaymentExecution paymentExecute = new PaymentExecution();
            paymentExecute.setPayerId(payerId);
            
            // Создаем платеж для захвата
            Payment payment = new Payment();
            payment.setId(paymentId);
            
            // Захватываем платеж
            Payment capturedPayment = payment.execute(apiContext, paymentExecute);
            log.info("PayPal payment captured successfully: {}", capturedPayment.getId());
            
            return capturedPayment;
        } catch (PayPalRESTException e) {
            log.error("Failed to capture PayPal payment: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public boolean isOrderApproved(Payment payment) {
        if (payment == null) {
            log.warn("Payment is null, cannot determine approval status");
            return false;
        }
        
        String state = payment.getState();
        log.debug("Payment state: {}", state);
        
        // Проверяем, что платеж одобрен
        return "approved".equalsIgnoreCase(state);
    }
} 
package com.ecommerce.lambda.service;

import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@Import(ContextFunctionCatalogAutoConfiguration.class)
public class PayPalService {
    private final APIContext apiContext;

    public PayPalService(String clientId, String clientSecret, String mode) {
        this.apiContext = new APIContext(clientId, clientSecret, mode);
    }

    @Bean
    public PayPalService payPalService() {
        return new PayPalService(
            System.getenv("PAYPAL_CLIENT_ID"),
            System.getenv("PAYPAL_CLIENT_SECRET"),
            System.getenv("PAYPAL_MODE")
        );
    }

    public Payment createOrder(double amount, String currency, String orderNumber) throws PayPalRESTException {
        Amount amountObj = new Amount();
        amountObj.setCurrency(currency);
        amount = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
        amountObj.setTotal(String.format("%.2f", amount));

        Transaction transaction = new Transaction();
        transaction.setDescription("Order #" + orderNumber);
        transaction.setAmount(amountObj);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        return payment.create(apiContext);
    }

    public Payment captureOrder(String paymentId, String payerId) throws PayPalRESTException {
        Payment payment = new Payment();
        payment.setId(paymentId);
        PaymentExecution paymentExecute = new PaymentExecution();
        paymentExecute.setPayerId(payerId);
        return payment.execute(apiContext, paymentExecute);
    }

    public boolean isOrderApproved(Payment payment) {
        return payment.getState().equals("approved");
    }
} 
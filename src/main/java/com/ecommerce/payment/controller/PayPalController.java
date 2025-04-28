package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.PayPalPaymentResponse;
import com.ecommerce.payment.service.PayPalService;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments/paypal")
public class PayPalController {

    @Autowired
    private PayPalService payPalService;

    @PostMapping("/create")
    public ResponseEntity<PayPalPaymentResponse> createPayment(
            @RequestParam("amount") Double amount,
            @RequestParam("currency") String currency,
            @RequestParam("description") String description,
            @RequestParam(value = "successUrl", required = false, defaultValue = "http://localhost:3000/payment/success") String successUrl,
            @RequestParam(value = "cancelUrl", required = false, defaultValue = "http://localhost:3000/payment/cancel") String cancelUrl) {
        try {
            Payment payment = payPalService.createPayment(
                    amount,
                    currency,
                    "paypal",
                    "sale",
                    description,
                    cancelUrl,
                    successUrl
            );

            return ResponseEntity.ok(PayPalPaymentResponse.success(payment));
        } catch (PayPalRESTException e) {
            return ResponseEntity.badRequest().body(PayPalPaymentResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/execute")
    public ResponseEntity<PayPalPaymentResponse> executePayment(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId) {
        try {
            Payment payment = payPalService.executePayment(paymentId, payerId);
            
            if (payment.getState().equals("approved")) {
                return ResponseEntity.ok(PayPalPaymentResponse.success(payment));
            } else {
                return ResponseEntity.badRequest()
                        .body(PayPalPaymentResponse.error("Payment status: " + payment.getState()));
            }
        } catch (PayPalRESTException e) {
            return ResponseEntity.badRequest().body(PayPalPaymentResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/cancel")
    public ResponseEntity<PayPalPaymentResponse> cancelPayment() {
        PayPalPaymentResponse response = new PayPalPaymentResponse();
        response.setStatus("CANCELLED");
        response.setErrorMessage("Payment was cancelled by user");
        return ResponseEntity.ok(response);
    }
} 
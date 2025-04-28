package com.ecommerce.payment.dto;

import com.paypal.api.payments.Payment;
import lombok.Data;

@Data
public class PayPalPaymentResponse {
    private String paymentId;
    private String status;
    private String redirectUrl;
    private String errorMessage;
    private PaymentDetails paymentDetails;

    @Data
    public static class PaymentDetails {
        private String currency;
        private String amount;
        private String description;
        private String createTime;
        private String updateTime;
    }

    public static PayPalPaymentResponse success(Payment payment) {
        PayPalPaymentResponse response = new PayPalPaymentResponse();
        response.setPaymentId(payment.getId());
        response.setStatus(payment.getState());
        
        PaymentDetails details = new PaymentDetails();
        if (payment.getTransactions() != null && !payment.getTransactions().isEmpty()) {
            com.paypal.api.payments.Transaction transaction = payment.getTransactions().get(0);
            details.setAmount(transaction.getAmount().getTotal());
            details.setCurrency(transaction.getAmount().getCurrency());
            details.setDescription(transaction.getDescription());
        }
        details.setCreateTime(payment.getCreateTime());
        details.setUpdateTime(payment.getUpdateTime());
        response.setPaymentDetails(details);

        if (payment.getLinks() != null) {
            for (com.paypal.api.payments.Links link : payment.getLinks()) {
                if (link.getRel().equals("approval_url")) {
                    response.setRedirectUrl(link.getHref());
                    break;
                }
            }
        }

        return response;
    }

    public static PayPalPaymentResponse error(String errorMessage) {
        PayPalPaymentResponse response = new PayPalPaymentResponse();
        response.setStatus("ERROR");
        response.setErrorMessage(errorMessage);
        return response;
    }
} 
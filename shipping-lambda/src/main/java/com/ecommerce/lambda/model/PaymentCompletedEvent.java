package com.ecommerce.lambda.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentCompletedEvent {
    private String orderId;
    private String orderNumber;
    private String paymentId;
    private String paymentStatus;
    private String paymentMethod;
    private String paymentDate;
} 
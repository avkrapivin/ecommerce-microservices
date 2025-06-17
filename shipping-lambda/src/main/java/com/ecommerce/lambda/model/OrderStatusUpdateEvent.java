package com.ecommerce.lambda.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateEvent {
    private String orderId;
    private String status;
    private String trackingNumber;
} 
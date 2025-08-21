package com.ecommerce.shipping.dto;

import com.ecommerce.shipping.model.ShippingStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShippingInfoDto {
    private Long id;
    private Long orderId;
    private String shippoShipmentId;
    private String shippoRateId;
    private String shippoTransactionId;
    private String trackingNumber;
    private String trackingUrl;
    private String labelUrl;
    private String carrier;
    private String service;
    private String amount;
    private String currency;
    private String estimatedDays;
    private ShippingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 
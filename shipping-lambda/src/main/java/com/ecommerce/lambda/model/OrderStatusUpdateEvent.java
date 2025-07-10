package com.ecommerce.lambda.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Событие обновления статуса заказа
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateEvent {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("orderNumber")
    private String orderNumber;
    
    @JsonProperty("customerEmail")
    private String customerEmail;
    
    @JsonProperty("customerName")
    private String customerName;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("previousStatus")
    private String previousStatus;
    
    @JsonProperty("updatedAt")
    private String updatedAt;
    
    @JsonProperty("trackingNumber")
    private String trackingNumber;
    
    @JsonProperty("notes")
    private String notes;
    
    /**
     * Конструктор для создания события с основными параметрами
     */
    public OrderStatusUpdateEvent(String orderId, String status, String trackingNumber) {
        this.orderId = orderId;
        this.status = status;
        this.trackingNumber = trackingNumber;
    }
} 
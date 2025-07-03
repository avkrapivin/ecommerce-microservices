package com.ecommerce.lambda.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderReadyForDeliveryEvent {
    private String orderId;
    private String orderNumber;
    private String customerEmail;
    private String customerName;
    private String shippingAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingZip;
    private String shippingCountry;
    private String phoneNumber;
    private Double totalAmount;
    private String currency;
    
    // Параметры посылки
    private Double parcelLength;
    private Double parcelWidth;
    private Double parcelHeight;
    private String parcelDistanceUnit;
    private Double parcelWeight;
    private String parcelMassUnit;
} 
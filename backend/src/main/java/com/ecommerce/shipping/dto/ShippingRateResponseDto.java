package com.ecommerce.shipping.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ShippingRateResponseDto {
    private List<ShippingRateDto> rates;
    private String error; // Если есть ошибка при расчете
    
    @Data
    public static class ShippingRateDto {
        private String objectId;
        private String provider;
        private String service;
        private String currency;
        private String amount;
        private String days;
        private String estimatedDays;
        private String durationTerms;
        private String providerImage75;
        private String providerImage200;
        private String trackingNumber;
        private String trackingUrlProvider;
    }
} 
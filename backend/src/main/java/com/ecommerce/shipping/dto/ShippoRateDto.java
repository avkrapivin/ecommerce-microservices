package com.ecommerce.shipping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ShippoRateDto {
    private String objectId;
    private String provider;
    private String service;
    private String currency;
    private String amount;
    private String days;
    
    @JsonProperty("estimated_days")
    private String estimatedDays;
    
    @JsonProperty("duration_terms")
    private String durationTerms;
    
    @JsonProperty("provider_image_75")
    private String providerImage75;
    
    @JsonProperty("provider_image_200")
    private String providerImage200;
} 
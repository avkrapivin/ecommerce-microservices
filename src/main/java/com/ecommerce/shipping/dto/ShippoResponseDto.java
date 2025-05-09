package com.ecommerce.shipping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ShippoResponseDto {
    private String objectId;
    private String objectState;
    private String status;
    private String message;
    
    @JsonProperty("label_url")
    private String labelUrl;
    
    @JsonProperty("tracking_number")
    private String trackingNumber;
    
    @JsonProperty("tracking_url_provider")
    private String trackingUrlProvider;
    
    @JsonProperty("object_created")
    private String objectCreated;
    
    @JsonProperty("object_updated")
    private String objectUpdated;
} 
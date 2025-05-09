package com.ecommerce.shipping.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ShippoTrackingDto {
    private String objectId;
    private String status;
    private String message;
    
    @JsonProperty("tracking_number")
    private String trackingNumber;
    
    @JsonProperty("tracking_url_provider")
    private String trackingUrlProvider;
    
    @JsonProperty("tracking_status")
    private TrackingStatus trackingStatus;
    
    @JsonProperty("tracking_history")
    private List<TrackingHistory> trackingHistory;
    
    @JsonProperty("object_created")
    private LocalDateTime objectCreated;
    
    @JsonProperty("object_updated")
    private LocalDateTime objectUpdated;
    
    @Data
    public static class TrackingStatus {
        private String status;
        private String statusDetails;
        private String statusDate;
        private String location;
    }
    
    @Data
    public static class TrackingHistory {
        private String status;
        private String statusDetails;
        private String statusDate;
        private String location;
    }
} 
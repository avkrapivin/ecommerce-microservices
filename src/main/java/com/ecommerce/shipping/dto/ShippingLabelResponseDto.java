package com.ecommerce.shipping.dto;

import lombok.Data;
import java.util.List;

@Data
public class ShippingLabelResponseDto {
    private String objectId;
    private String status;
    private String message;
    private String labelUrl;
    private String trackingNumber;
    private String trackingUrlProvider;
    private String labelFileType;
    private String labelSize;
    private String labelResolution;
    private String labelFileSize;
    private String error; // Если есть ошибка при генерации
} 
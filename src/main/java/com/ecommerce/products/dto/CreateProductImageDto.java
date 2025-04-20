package com.ecommerce.products.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateProductImageDto {
    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    private String altText;

    @NotNull(message = "Display order is required")
    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder;

    private boolean isPrimary = false;
} 
package com.ecommerce.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductRequest {
    @NotNull(message = "Product ID is required")
    private Long id;
} 
package com.ecommerce.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemRequest {
    @NotNull(message = "Product is required")
    private ProductRequest product;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
} 
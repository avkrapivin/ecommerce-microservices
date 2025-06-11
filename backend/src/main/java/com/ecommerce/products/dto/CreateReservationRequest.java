package com.ecommerce.products.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class CreateReservationRequest {
    @NotNull(message = "ID product is required")
    private Long productId;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer quantity;
} 
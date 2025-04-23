package com.ecommerce.products.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductSpecificationDto {
    private Long id;
    private Long productId;
    @NotBlank(message = "Specification name is required")
    private String name;
    @NotBlank(message = "Specification value is required")
    private String specValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 
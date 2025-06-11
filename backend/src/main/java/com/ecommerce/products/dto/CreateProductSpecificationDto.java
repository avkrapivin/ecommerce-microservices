package com.ecommerce.products.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateProductSpecificationDto {
    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Specification value is required")
    @Size(min = 1, max = 500, message = "Value must be between 1 and 500 characters")
    private String specValue;

    @NotNull(message = "Display order is required")
    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder;

    private String unit;
} 
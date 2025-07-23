package com.ecommerce.products.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductSpecificationDto extends BaseSpecificationDto {
    private Long id;
    private Long productId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 
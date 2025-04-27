package com.ecommerce.products.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductReservationDto {
    private Long id;
    private Long productId;
    private Long userId;
    private Integer quantity;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private boolean active;
} 
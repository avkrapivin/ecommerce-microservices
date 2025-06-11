package com.ecommerce.products.dto;

import com.ecommerce.products.entity.Product;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductReviewDto {
    private Long id;
    private Product product;
    private Long userId;
    private String userName;
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 
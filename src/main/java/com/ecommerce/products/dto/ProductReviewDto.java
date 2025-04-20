package com.ecommerce.products.dto;

import lombok.Data;

@Data
public class ProductReviewDto {
    private Long id;
    private Long userId;
    private String userName;
    private Integer rating;
    private String comment;
    private String createdAt;
} 
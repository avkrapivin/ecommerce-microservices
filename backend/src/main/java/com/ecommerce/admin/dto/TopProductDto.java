package com.ecommerce.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopProductDto {
    private Long productId;
    private String productName;
    private String sku;
    private Integer unitsSold;
    private BigDecimal revenue;
    private String imageUrl;
    private String categoryName;
    private BigDecimal averageRating;
} 
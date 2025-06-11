package com.ecommerce.products.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private Long categoryId;
    private String categoryName;
    private String sku;
    private boolean active;
    private List<ProductImageDto> images;
    private List<ProductReviewDto> reviews;
    private List<ProductSpecificationDto> specifications;
    private String createdAt;
    private String updatedAt;
} 
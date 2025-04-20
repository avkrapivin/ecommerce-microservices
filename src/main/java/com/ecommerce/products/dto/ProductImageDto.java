package com.ecommerce.products.dto;

import lombok.Data;

@Data
public class ProductImageDto {
    private Long id;
    private Long productId;
    private String imageUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private boolean isMain;
    private String createdAt;
    private String updatedAt;
} 
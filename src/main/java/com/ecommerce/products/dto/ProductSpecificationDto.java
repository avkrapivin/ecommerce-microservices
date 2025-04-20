package com.ecommerce.products.dto;

import lombok.Data;

@Data
public class ProductSpecificationDto {
    private Long id;
    private String name;
    private String value;
    private Integer displayOrder;
} 
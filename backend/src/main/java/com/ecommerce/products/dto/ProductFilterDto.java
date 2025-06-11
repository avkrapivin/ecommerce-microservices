package com.ecommerce.products.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductFilterDto {
    private String search;
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private List<String> specifications;
    private String sortBy;
    private String sortDirection;
    private Integer page;
    private Integer size;
} 
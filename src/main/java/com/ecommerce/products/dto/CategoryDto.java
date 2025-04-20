package com.ecommerce.products.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoryDto {
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private String parentName;
    private List<CategoryDto> subcategories;
    private boolean active;
    private String createdAt;
    private String updatedAt;
} 
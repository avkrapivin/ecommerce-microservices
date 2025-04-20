package com.ecommerce.products.service;

import com.ecommerce.products.dto.CategoryDto;
import com.ecommerce.products.entity.Category;
import com.ecommerce.products.repository.CategoryRepository;
import com.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#id")
    public CategoryDto getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "categories")
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findByActiveTrue().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "#parentId")
    public List<CategoryDto> getSubcategories(Long parentId) {
        return categoryRepository.findActiveSubcategories(parentId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'root'")
    public List<CategoryDto> getRootCategories() {
        return categoryRepository.findRootCategories().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDto createCategory(CategoryDto categoryDto) {
        Category category = new Category();
        updateCategoryFromDto(category, categoryDto);
        return convertToDto(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        
        updateCategoryFromDto(category, categoryDto);
        return convertToDto(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    private CategoryDto convertToDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setParentId(category.getParent() != null ? category.getParent().getId() : null);
        dto.setParentName(category.getParent() != null ? category.getParent().getName() : null);
        dto.setActive(category.isActive());
        dto.setCreatedAt(category.getCreatedAt().toString());
        dto.setUpdatedAt(category.getUpdatedAt().toString());
        return dto;
    }

    private void updateCategoryFromDto(Category category, CategoryDto dto) {
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        if (dto.getParentId() != null) {
            category.setParent(categoryRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + dto.getParentId())));
        }
        category.setActive(dto.isActive());
    }
} 
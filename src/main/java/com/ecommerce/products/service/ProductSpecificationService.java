package com.ecommerce.products.service;

import com.ecommerce.products.dto.*;
import com.ecommerce.products.entity.ProductSpecification;
import com.ecommerce.products.repository.ProductSpecificationRepository;
import com.ecommerce.products.repository.ProductRepository;
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
public class ProductSpecificationService {
    private final ProductSpecificationRepository productSpecificationRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "specifications", key = "#productId")
    public List<ProductSpecificationDto> getProductSpecifications(Long productId) {
        return productSpecificationRepository.findByProductId(productId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"specifications", "products"}, allEntries = true)
    public ProductSpecificationDto createSpecification(CreateProductSpecificationDto createSpecificationDto) {
        ProductSpecification specification = new ProductSpecification();
        specification.setProduct(productRepository.findById(createSpecificationDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + createSpecificationDto.getProductId())));
        specification.setName(createSpecificationDto.getName());
        specification.setValue(createSpecificationDto.getValue());
        specification.setDisplayOrder(createSpecificationDto.getDisplayOrder());

        return convertToDto(productSpecificationRepository.save(specification));
    }

    @Transactional
    @CacheEvict(value = {"specifications", "products"}, allEntries = true)
    public ProductSpecificationDto updateSpecification(Long id, UpdateProductSpecificationDto updateSpecificationDto) {
        ProductSpecification specification = productSpecificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specification not found with id: " + id));

        specification.setName(updateSpecificationDto.getName());
        specification.setValue(updateSpecificationDto.getValue());
        specification.setDisplayOrder(updateSpecificationDto.getDisplayOrder());

        return convertToDto(productSpecificationRepository.save(specification));
    }

    @Transactional
    @CacheEvict(value = {"specifications", "products"}, allEntries = true)
    public void deleteSpecification(Long id) {
        if (!productSpecificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Specification not found with id: " + id);
        }
        productSpecificationRepository.deleteById(id);
    }

    private ProductSpecificationDto convertToDto(ProductSpecification specification) {
        ProductSpecificationDto dto = new ProductSpecificationDto();
        dto.setId(specification.getId());
        dto.setName(specification.getName());
        dto.setValue(specification.getValue());
        dto.setDisplayOrder(specification.getDisplayOrder());
        return dto;
    }
} 
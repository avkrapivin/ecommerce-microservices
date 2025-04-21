package com.ecommerce.products.service;

import com.ecommerce.products.dto.ProductSpecificationDto;
import com.ecommerce.products.entity.Product;
import com.ecommerce.products.entity.ProductSpecification;
import com.ecommerce.products.repository.ProductRepository;
import com.ecommerce.products.repository.ProductSpecificationRepository;
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
    private final ProductSpecificationRepository specificationRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "productSpecifications", key = "'product:' + #productId")
    public List<ProductSpecificationDto> getProductSpecifications(Long productId) {
        return specificationRepository.findByProductId(productId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "productSpecifications", key = "'product:' + #productId")
    public ProductSpecificationDto createSpecification(Long productId, ProductSpecificationDto specificationDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        ProductSpecification specification = new ProductSpecification();
        specification.setProduct(product);
        specification.setName(specificationDto.getName());
        specification.setValue(specificationDto.getValue());

        return convertToDto(specificationRepository.save(specification));
    }

    @Transactional
    @CacheEvict(value = "productSpecifications", key = "'product:' + #productId")
    public ProductSpecificationDto updateSpecification(Long productId, Long specificationId, ProductSpecificationDto specificationDto) {
        ProductSpecification specification = specificationRepository.findById(specificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Specification not found with id: " + specificationId));

        if (!specification.getProduct().getId().equals(productId)) {
            throw new IllegalStateException("Specification does not belong to the specified product");
        }

        specification.setName(specificationDto.getName());
        specification.setValue(specificationDto.getValue());

        return convertToDto(specificationRepository.save(specification));
    }

    @Transactional
    @CacheEvict(value = "productSpecifications", key = "'product:' + #productId")
    public void deleteSpecification(Long productId, Long specificationId) {
        ProductSpecification specification = specificationRepository.findById(specificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Specification not found with id: " + specificationId));

        if (!specification.getProduct().getId().equals(productId)) {
            throw new IllegalStateException("Specification does not belong to the specified product");
        }

        specificationRepository.delete(specification);
    }

    private ProductSpecificationDto convertToDto(ProductSpecification specification) {
        ProductSpecificationDto dto = new ProductSpecificationDto();
        dto.setId(specification.getId());
        dto.setProductId(specification.getProduct().getId());
        dto.setName(specification.getName());
        dto.setValue(specification.getValue());
        dto.setCreatedAt(specification.getCreatedAt());
        dto.setUpdatedAt(specification.getUpdatedAt());
        return dto;
    }
} 
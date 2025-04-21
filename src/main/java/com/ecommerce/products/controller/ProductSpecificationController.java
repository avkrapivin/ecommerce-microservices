package com.ecommerce.products.controller;

import com.ecommerce.products.dto.ProductSpecificationDto;
import com.ecommerce.products.service.ProductSpecificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products/{productId}/specifications")
@RequiredArgsConstructor
public class ProductSpecificationController {
    private final ProductSpecificationService specificationService;

    @GetMapping
    public ResponseEntity<List<ProductSpecificationDto>> getProductSpecifications(@PathVariable Long productId) {
        return ResponseEntity.ok(specificationService.getProductSpecifications(productId));
    }

    @PostMapping
    public ResponseEntity<ProductSpecificationDto> createSpecification(
            @PathVariable Long productId,
            @RequestBody ProductSpecificationDto specificationDto) {
        return ResponseEntity.ok(specificationService.createSpecification(productId, specificationDto));
    }

    @PutMapping("/{specificationId}")
    public ResponseEntity<ProductSpecificationDto> updateSpecification(
            @PathVariable Long productId,
            @PathVariable Long specificationId,
            @RequestBody ProductSpecificationDto specificationDto) {
        return ResponseEntity.ok(specificationService.updateSpecification(productId, specificationId, specificationDto));
    }

    @DeleteMapping("/{specificationId}")
    public ResponseEntity<Void> deleteSpecification(
            @PathVariable Long productId,
            @PathVariable Long specificationId) {
        specificationService.deleteSpecification(productId, specificationId);
        return ResponseEntity.noContent().build();
    }
} 
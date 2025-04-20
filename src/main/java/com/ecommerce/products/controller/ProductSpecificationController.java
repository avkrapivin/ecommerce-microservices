package com.ecommerce.products.controller;

import com.ecommerce.products.dto.*;
import com.ecommerce.products.service.ProductSpecificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/specifications")
@RequiredArgsConstructor
public class ProductSpecificationController {
    private final ProductSpecificationService productSpecificationService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductSpecificationDto>> getProductSpecifications(@PathVariable Long productId) {
        return ResponseEntity.ok(productSpecificationService.getProductSpecifications(productId));
    }

    @PostMapping
    public ResponseEntity<ProductSpecificationDto> createSpecification(@Valid @RequestBody CreateProductSpecificationDto createSpecificationDto) {
        ProductSpecificationDto createdSpecification = productSpecificationService.createSpecification(createSpecificationDto);
        return ResponseEntity
                .created(URI.create("/specifications/" + createdSpecification.getId()))
                .body(createdSpecification);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductSpecificationDto> updateSpecification(@PathVariable Long id, @Valid @RequestBody UpdateProductSpecificationDto updateSpecificationDto) {
        return ResponseEntity.ok(productSpecificationService.updateSpecification(id, updateSpecificationDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpecification(@PathVariable Long id) {
        productSpecificationService.deleteSpecification(id);
        return ResponseEntity.ok().build();
    }
} 
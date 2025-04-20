package com.ecommerce.products.controller;

import com.ecommerce.products.dto.ProductImageDto;
import com.ecommerce.products.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/products/{productId}/images")
@RequiredArgsConstructor
public class ProductImageController {
    private final ProductImageService productImageService;

    @GetMapping
    public ResponseEntity<List<ProductImageDto>> getProductImages(@PathVariable Long productId) {
        return ResponseEntity.ok(productImageService.getProductImages(productId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductImageDto> uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isMain", defaultValue = "false") boolean isMain) throws IOException {
        return ResponseEntity.ok(productImageService.uploadImage(productId, file, isMain));
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) throws IOException {
        productImageService.deleteImage(productId, imageId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{imageId}/main")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductImageDto> setMainImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        return ResponseEntity.ok(productImageService.setMainImage(productId, imageId));
    }
} 
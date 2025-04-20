package com.ecommerce.products.controller;

import com.ecommerce.products.dto.*;
import com.ecommerce.products.service.ProductReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ProductReviewController {
    private final ProductReviewService productReviewService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductReviewDto>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(productReviewService.getProductReviews(productId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ProductReviewDto>> getUserReviews(@PathVariable Long userId) {
        return ResponseEntity.ok(productReviewService.getUserReviews(userId));
    }

    @PostMapping
    public ResponseEntity<ProductReviewDto> createReview(@Valid @RequestBody CreateProductReviewDto createReviewDto) {
        ProductReviewDto createdReview = productReviewService.createReview(createReviewDto);
        return ResponseEntity
                .created(URI.create("/reviews/" + createdReview.getId()))
                .body(createdReview);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductReviewDto> updateReview(@PathVariable Long id, @Valid @RequestBody UpdateProductReviewDto updateReviewDto) {
        return ResponseEntity.ok(productReviewService.updateReview(id, updateReviewDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        productReviewService.deleteReview(id);
        return ResponseEntity.ok().build();
    }
} 
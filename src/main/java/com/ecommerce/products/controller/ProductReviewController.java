package com.ecommerce.products.controller;

import com.ecommerce.products.dto.ProductReviewDto;
import com.ecommerce.products.service.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {
    private final ProductReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<ProductReviewDto>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId));
    }

    @PostMapping
    public ResponseEntity<ProductReviewDto> createReview(
            @PathVariable Long productId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody ProductReviewDto reviewDto) {
        return ResponseEntity.ok(reviewService.createReview(productId, userId, reviewDto));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ProductReviewDto> updateReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody ProductReviewDto reviewDto) {
        return ResponseEntity.ok(reviewService.updateReview(productId, reviewId, userId, reviewDto));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long productId,
            @PathVariable Long reviewId,
            @RequestHeader("X-User-Id") Long userId) {
        reviewService.deleteReview(productId, reviewId, userId);
        return ResponseEntity.noContent().build();
    }
} 
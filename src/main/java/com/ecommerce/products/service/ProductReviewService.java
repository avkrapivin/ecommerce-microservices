package com.ecommerce.products.service;

import com.ecommerce.products.dto.ProductReviewDto;
import com.ecommerce.products.entity.Product;
import com.ecommerce.products.entity.ProductReview;
import com.ecommerce.products.repository.ProductRepository;
import com.ecommerce.products.repository.ProductReviewRepository;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
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
public class ProductReviewService {
    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    @Cacheable(value = "productReviews", key = "'product:' + #productId")
    public List<ProductReviewDto> getProductReviews(Long productId) {
        return reviewRepository.findByProductId(productId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "productReviews", key = "'product:' + #productId")
    public ProductReviewDto createReview(Long productId, Long userId, ProductReviewDto reviewDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        User user = userService.getUserById(userId);

        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            throw new IllegalStateException("User has already reviewed this product");
        }

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());

        return convertToDto(reviewRepository.save(review));
    }

    @Transactional
    @CacheEvict(value = "productReviews", key = "'product:' + #productId")
    public ProductReviewDto updateReview(Long productId, Long reviewId, Long userId, ProductReviewDto reviewDto) {
        ProductReview review = reviewRepository.findByIdAndProductId(reviewId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalStateException("User can only update their own reviews");
        }

        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());

        return convertToDto(reviewRepository.save(review));
    }

    @Transactional
    @CacheEvict(value = "productReviews", key = "'product:' + #productId")
    public void deleteReview(Long productId, Long reviewId, Long userId) {
        ProductReview review = reviewRepository.findByIdAndProductId(reviewId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalStateException("User can only delete their own reviews");
        }

        reviewRepository.delete(review);
    }

    private ProductReviewDto convertToDto(ProductReview review) {
        ProductReviewDto dto = new ProductReviewDto();
        dto.setId(review.getId());
        dto.setProduct(review.getProduct());
        dto.setUserId(review.getUser().getId());
        dto.setUserName(review.getUser().getEmail());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        return dto;
    }
} 
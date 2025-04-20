package com.ecommerce.products.service;

import com.ecommerce.products.dto.*;
import com.ecommerce.products.entity.ProductReview;
import com.ecommerce.products.repository.ProductReviewRepository;
import com.ecommerce.products.repository.ProductRepository;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import com.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductReviewService {
    private final ProductReviewRepository productReviewRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Transactional(readOnly = true)
    @Cacheable(value = "productReviews", key = "'product:' + #productId")
    public List<ProductReviewDto> getProductReviews(Long productId) {
        return productReviewRepository.findByProductId(productId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "userReviews", key = "'user:' + #userId")
    public List<ProductReviewDto> getUserReviews(Long userId) {
        return productReviewRepository.findByUserId(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"productReviews", "userReviews"}, allEntries = true)
    public ProductReviewDto createReview(CreateProductReviewDto createReviewDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.getUserByEmail(authentication.getName());

        ProductReview review = new ProductReview();
        review.setProduct(productRepository.findById(createReviewDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + createReviewDto.getProductId())));
        review.setUser(user);
        review.setRating(createReviewDto.getRating());
        review.setComment(createReviewDto.getComment());

        return convertToDto(productReviewRepository.save(review));
    }

    @Transactional
    @CacheEvict(value = {"productReviews", "userReviews"}, allEntries = true)
    public ProductReviewDto updateReview(Long id, UpdateProductReviewDto updateReviewDto) {
        ProductReview review = productReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));

        User user = getAuthenticatedUser();
        verifyUserOwnership(review, user);

        review.setRating(updateReviewDto.getRating());
        review.setComment(updateReviewDto.getComment());

        return convertToDto(productReviewRepository.save(review));
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userService.getUserByEmail(authentication.getName());
    }

    private void verifyUserOwnership(ProductReview review, User user) {
        if (!review.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("You can only update your own reviews");
        }
    }

    @Transactional
    @CacheEvict(value = {"productReviews", "userReviews"}, allEntries = true)
    public void deleteReview(Long id) {
        ProductReview review = productReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));

        User user = getAuthenticatedUser();
        verifyUserOwnership(review, user);

        productReviewRepository.deleteById(id);
    }

    private ProductReviewDto convertToDto(ProductReview review) {
        ProductReviewDto dto = new ProductReviewDto();
        dto.setId(review.getId());
        dto.setUserId(review.getUser().getId());
        dto.setUserName(review.getUser().getFirstName() + " " + review.getUser().getLastName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt().format(FORMATTER));
        return dto;
    }
} 
package com.ecommerce.products.service;

import com.ecommerce.products.dto.*;
import com.ecommerce.products.entity.*;
import com.ecommerce.products.repository.*;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import com.ecommerce.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceUnitTest {
    @Mock
    private ProductReviewRepository productReviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProductReviewService productReviewService;

    @Test
    void getProductReviews_ShouldReturnReviews() {
        // Given
        Product product = new Product();
        product.setId(1L);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        ProductReview review = new ProductReview();
        review.setId(1L);
        review.setRating(5);
        review.setComment("Great product!");
        review.setProduct(product);
        review.setUser(user);

        List<ProductReview> reviews = Collections.singletonList(review);

        when(productReviewRepository.findByProductId(1L)).thenReturn(reviews);

        // When
        List<ProductReviewDto> result = productReviewService.getProductReviews(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getRating());
        assertEquals("Great product!", result.get(0).getComment());
        assertEquals(1L, result.get(0).getUserId());
        verify(productReviewRepository, times(1)).findByProductId(1L);
    }

    @Test
    void createReview_ShouldCreateReviewSuccessfully() {
        // Given
        Product product = new Product();
        product.setId(1L);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        ProductReviewDto reviewDto = new ProductReviewDto();
        reviewDto.setRating(5);
        reviewDto.setComment("Great product!");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userService.getUserById(1L)).thenReturn(user);
        when(productReviewRepository.existsByProductIdAndUserId(1L, 1L)).thenReturn(false);
        when(productReviewRepository.save(any(ProductReview.class))).thenAnswer(invocation -> {
            ProductReview review = invocation.getArgument(0);
            review.setId(1L);
            return review;
        });

        // When
        ProductReviewDto result = productReviewService.createReview(1L, 1L, reviewDto);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("Great product!", result.getComment());
        assertEquals(1L, result.getUserId());
        verify(productRepository, times(1)).findById(1L);
        verify(userService, times(1)).getUserById(1L);
        verify(productReviewRepository, times(1)).existsByProductIdAndUserId(1L, 1L);
        verify(productReviewRepository, times(1)).save(any(ProductReview.class));
    }

    @Test
    void createReview_ShouldThrowException_WhenUserAlreadyReviewed() {
        // Given
        Product product = new Product();
        product.setId(1L);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        ProductReviewDto reviewDto = new ProductReviewDto();
        reviewDto.setRating(5);
        reviewDto.setComment("Great product!");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userService.getUserById(1L)).thenReturn(user);
        when(productReviewRepository.existsByProductIdAndUserId(1L, 1L)).thenReturn(true);

        // When & Then
        assertThrows(IllegalStateException.class, () ->
            productReviewService.createReview(1L, 1L, reviewDto)
        );
        verify(productRepository, times(1)).findById(1L);
        verify(userService, times(1)).getUserById(1L);
        verify(productReviewRepository, times(1)).existsByProductIdAndUserId(1L, 1L);
        verify(productReviewRepository, never()).save(any(ProductReview.class));
    }

    @Test
    void updateReview_ShouldUpdateReviewSuccessfully() {
        // Given
        Product product = new Product();
        product.setId(1L);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        ProductReview existingReview = new ProductReview();
        existingReview.setId(1L);
        existingReview.setRating(3);
        existingReview.setComment("Average product");
        existingReview.setProduct(product);
        existingReview.setUser(user);

        ProductReviewDto reviewDto = new ProductReviewDto();
        reviewDto.setRating(5);
        reviewDto.setComment("Great product!");

        when(productReviewRepository.findByIdAndProductId(1L, 1L)).thenReturn(Optional.of(existingReview));
        when(productReviewRepository.save(any(ProductReview.class))).thenReturn(existingReview);

        // When
        ProductReviewDto result = productReviewService.updateReview(1L, 1L, 1L, reviewDto);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("Great product!", result.getComment());
        assertEquals(1L, result.getUserId());
        verify(productReviewRepository, times(1)).findByIdAndProductId(1L, 1L);
        verify(productReviewRepository, times(1)).save(any(ProductReview.class));
    }

    @Test
    void updateReview_ShouldThrowException_WhenNotReviewOwner() {
        // Given
        Product product = new Product();
        product.setId(1L);

        User user = new User();
        user.setId(1L);
        user.setEmail("owner@example.com");

        ProductReview existingReview = new ProductReview();
        existingReview.setId(1L);
        existingReview.setProduct(product);
        existingReview.setUser(user);

        ProductReviewDto reviewDto = new ProductReviewDto();
        reviewDto.setRating(5);
        reviewDto.setComment("Great product!");

        when(productReviewRepository.findByIdAndProductId(1L, 1L)).thenReturn(Optional.of(existingReview));

        // When & Then
        assertThrows(IllegalStateException.class, () ->
            productReviewService.updateReview(1L, 1L, 2L, reviewDto)
        );
        verify(productReviewRepository, times(1)).findByIdAndProductId(1L, 1L);
        verify(productReviewRepository, never()).save(any(ProductReview.class));
    }

    @Test
    void deleteReview_ShouldDeleteReviewSuccessfully() {
        // Given
        Product product = new Product();
        product.setId(1L);

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        ProductReview review = new ProductReview();
        review.setId(1L);
        review.setProduct(product);
        review.setUser(user);

        when(productReviewRepository.findByIdAndProductId(1L, 1L)).thenReturn(Optional.of(review));
        doNothing().when(productReviewRepository).delete(review);

        // When
        productReviewService.deleteReview(1L, 1L, 1L);

        // Then
        verify(productReviewRepository, times(1)).findByIdAndProductId(1L, 1L);
        verify(productReviewRepository, times(1)).delete(review);
    }

    @Test
    void deleteReview_ShouldThrowException_WhenNotReviewOwner() {
        // Given
        Product product = new Product();
        product.setId(1L);

        User user = new User();
        user.setId(1L);
        user.setEmail("owner@example.com");

        ProductReview review = new ProductReview();
        review.setId(1L);
        review.setProduct(product);
        review.setUser(user);

        when(productReviewRepository.findByIdAndProductId(1L, 1L)).thenReturn(Optional.of(review));

        // When & Then
        assertThrows(IllegalStateException.class, () ->
            productReviewService.deleteReview(1L, 1L, 2L)
        );
        verify(productReviewRepository, times(1)).findByIdAndProductId(1L, 1L);
        verify(productReviewRepository, never()).delete(any(ProductReview.class));
    }
} 
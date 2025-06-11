package com.ecommerce.products.service;

import com.ecommerce.products.dto.ProductReviewDto;
import com.ecommerce.products.entity.Product;
import com.ecommerce.products.entity.ProductReview;
import com.ecommerce.user.entity.User;
import com.ecommerce.products.repository.ProductRepository;
import com.ecommerce.products.repository.ProductReviewRepository;
import com.ecommerce.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceTest {

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProductReviewService reviewService;

    private Product product;
    private User user;
    private ProductReview review;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        review = new ProductReview();
        review.setId(1L);
        review.setProduct(product);
        review.setUser(user);
        review.setRating(5);
        review.setComment("Great product!");
    }

    @Test
    void createReview_ShouldCreateNewReview() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userService.getUserById(1L)).thenReturn(user);
        when(reviewRepository.existsByProductIdAndUserId(1L, 1L)).thenReturn(false);
        when(reviewRepository.save(any(ProductReview.class))).thenReturn(review);

        ProductReviewDto reviewDto = new ProductReviewDto();
        reviewDto.setRating(5);
        reviewDto.setComment("Great product!");

        ProductReviewDto result = reviewService.createReview(1L, 1L, reviewDto);

        assertNotNull(result);
        assertEquals(5, result.getRating());
        assertEquals("Great product!", result.getComment());
        verify(reviewRepository).save(any(ProductReview.class));
    }

    @Test
    void createReview_ShouldThrowException_WhenUserAlreadyReviewed() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userService.getUserById(1L)).thenReturn(user);
        when(reviewRepository.existsByProductIdAndUserId(1L, 1L)).thenReturn(true);

        ProductReviewDto reviewDto = new ProductReviewDto();
        reviewDto.setRating(5);
        reviewDto.setComment("Great product!");

        assertThrows(IllegalStateException.class, () -> 
            reviewService.createReview(1L, 1L, reviewDto)
        );
    }

    @Test
    void updateReview_ShouldUpdateReview() {
        when(reviewRepository.findByIdAndProductId(1L, 1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(ProductReview.class))).thenReturn(review);

        ProductReviewDto reviewDto = new ProductReviewDto();
        reviewDto.setRating(4);
        reviewDto.setComment("Updated review");

        ProductReviewDto result = reviewService.updateReview(1L, 1L, 1L, reviewDto);

        assertNotNull(result);
        assertEquals(4, result.getRating());
        assertEquals("Updated review", result.getComment());
        verify(reviewRepository).save(any(ProductReview.class));
    }

    @Test
    void updateReview_ShouldThrowException_WhenUserNotOwner() {
        when(reviewRepository.findByIdAndProductId(1L, 1L)).thenReturn(Optional.of(review));

        ProductReviewDto reviewDto = new ProductReviewDto();
        reviewDto.setRating(4);
        reviewDto.setComment("Updated review");

        assertThrows(IllegalStateException.class, () -> 
            reviewService.updateReview(1L, 1L, 2L, reviewDto)
        );
    }

    @Test
    void deleteReview_ShouldDeleteReview() {
        when(reviewRepository.findByIdAndProductId(1L, 1L)).thenReturn(Optional.of(review));

        reviewService.deleteReview(1L, 1L, 1L);

        verify(reviewRepository).delete(review);
    }

    @Test
    void deleteReview_ShouldThrowException_WhenUserNotOwner() {
        when(reviewRepository.findByIdAndProductId(1L, 1L)).thenReturn(Optional.of(review));

        assertThrows(IllegalStateException.class, () -> 
            reviewService.deleteReview(1L, 1L, 2L)
        );
    }
} 
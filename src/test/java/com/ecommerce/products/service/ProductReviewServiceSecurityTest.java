package com.ecommerce.products.service;

import com.ecommerce.products.dto.*;
import com.ecommerce.products.entity.*;
import com.ecommerce.products.repository.*;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.service.UserService;
import com.ecommerce.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceSecurityTest {
    @Mock
    private ProductReviewRepository productReviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserService userService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProductReviewService productReviewService;

    @Test
    void createReview_ShouldAllowAuthenticatedUser() {
        // Given
        Product product = new Product();
        product.setId(1L);

        User user = new User();
        user.setId(1L);
        user.setRole(Role.USER);

        ProductReviewDto reviewDto = new ProductReviewDto();
        reviewDto.setRating(5);
        reviewDto.setComment("Great product!");

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);

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
        verify(productReviewRepository, times(1)).save(any(ProductReview.class));
    }

    @Test
    void createReview_ShouldDenyUnauthenticatedUser() {
        // Given
        ProductReviewDto reviewDto = new ProductReviewDto();
        reviewDto.setRating(5);
        reviewDto.setComment("Great product!");

        lenient().when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
            productReviewService.createReview(1L, 1L, reviewDto)
        );
        verify(productReviewRepository, never()).save(any(ProductReview.class));
    }

    @Test
    void updateReview_ShouldAllowReviewOwner() {
        // Given
        Product product = new Product();
        product.setId(1L);

        User owner = new User();
        owner.setId(1L);
        owner.setRole(Role.USER);

        ProductReview existingReview = new ProductReview();
        existingReview.setId(1L);
        existingReview.setProduct(product);
        existingReview.setUser(owner);

        ProductReviewDto reviewDto = new ProductReviewDto();
        reviewDto.setRating(5);
        reviewDto.setComment("Great product!");

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(owner);
        SecurityContextHolder.setContext(securityContext);

        when(productReviewRepository.findByIdAndProductId(1L, 1L)).thenReturn(Optional.of(existingReview));
        when(productReviewRepository.save(any(ProductReview.class))).thenReturn(existingReview);

        // When
        ProductReviewDto result = productReviewService.updateReview(1L, 1L, 1L, reviewDto);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getRating());
        verify(productReviewRepository, times(1)).save(any(ProductReview.class));
    }

    @Test
    void updateReview_ShouldDenyNonOwner() {
        // Given
        Product product = new Product();
        product.setId(1L);


        User owner = new User();
        owner.setId(1L);
        owner.setRole(Role.USER);

        User nonOwner = new User();
        nonOwner.setId(2L);
        nonOwner.setRole(Role.USER);

        ProductReview existingReview = new ProductReview();
        existingReview.setId(1L);
        existingReview.setProduct(product);
        existingReview.setUser(owner);

        ProductReviewDto reviewDto = new ProductReviewDto();
        reviewDto.setRating(5);
        reviewDto.setComment("Great product!");

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(nonOwner);
        SecurityContextHolder.setContext(securityContext);

        when(productReviewRepository.findByIdAndProductId(1L, 1L)).thenReturn(Optional.of(existingReview));

        // When & Then
        assertThrows(IllegalStateException.class, () ->
            productReviewService.updateReview(1L, 1L, 2L, reviewDto)
        );
        verify(productReviewRepository, never()).save(any(ProductReview.class));
    }

    @Test
    void deleteReview_ShouldAllowReviewOwner() {
        // Given
        Product product = new Product();
        product.setId(1L);

        User owner = new User();
        owner.setId(1L);
        owner.setRole(Role.USER);

        ProductReview review = new ProductReview();
        review.setId(1L);
        review.setProduct(product);
        review.setUser(owner);

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(owner);
        SecurityContextHolder.setContext(securityContext);

        when(productReviewRepository.findByIdAndProductId(1L, 1L)).thenReturn(Optional.of(review));
        doNothing().when(productReviewRepository).delete(review);

        // When
        productReviewService.deleteReview(1L, 1L, 1L);

        // Then
        verify(productReviewRepository, times(1)).delete(review);
    }

    @Test
    void deleteReview_ShouldDenyNonOwner() {
        // Given
        Product product = new Product();
        product.setId(1L);

        User owner = new User();
        owner.setId(1L);
        owner.setRole(Role.USER);

        User nonOwner = new User();
        nonOwner.setId(2L);
        nonOwner.setRole(Role.USER);

        ProductReview review = new ProductReview();
        review.setId(1L);
        review.setProduct(product);
        review.setUser(owner);

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(nonOwner);
        SecurityContextHolder.setContext(securityContext);

        when(productReviewRepository.findByIdAndProductId(1L, 1L)).thenReturn(Optional.of(review));

        // When & Then
        assertThrows(IllegalStateException.class, () ->
            productReviewService.deleteReview(1L, 1L, 2L)
        );
        verify(productReviewRepository, never()).delete(any(ProductReview.class));
    }
} 
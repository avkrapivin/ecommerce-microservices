package com.ecommerce.products.service;

import com.ecommerce.common.exception.InsufficientStockException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.products.entity.Product;
import com.ecommerce.products.entity.ProductReservation;
import com.ecommerce.products.repository.ProductReservationRepository;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductReservationServiceTest {
    @Mock
    private ProductReservationRepository productReservationRepository;
    
    @Mock
    private ProductService productService;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private ProductReservationService productReservationService;
    
    private Product testProduct;
    private User testUser;
    private ProductReservation testReservation;
    
    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setStockQuantity(10);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        
        testReservation = new ProductReservation();
        testReservation.setId(1L);
        testReservation.setProduct(testProduct);
        testReservation.setUser(testUser);
        testReservation.setQuantity(2);
        testReservation.setReservedAt(LocalDateTime.now());
        testReservation.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        testReservation.setActive(true);
    }
    
    @Test
    void reserveProduct_ShouldCreateReservation() {
        when(productService.getProductEntityById(1L)).thenReturn(testProduct);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(productReservationRepository.findByProductIdAndActiveTrue(1L))
            .thenReturn(List.of());
        when(productReservationRepository.save(any(ProductReservation.class)))
            .thenReturn(testReservation);
        
        ProductReservation result = productReservationService.reserveProduct(1L, 1L, 2);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(2, result.getQuantity());
        assertTrue(result.isActive());
        
        verify(productReservationRepository).save(any(ProductReservation.class));
    }
    
    @Test
    void reserveProduct_ShouldThrowException_WhenInsufficientStock() {
        when(productService.getProductEntityById(1L)).thenReturn(testProduct);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(productReservationRepository.findByProductIdAndActiveTrue(1L))
            .thenReturn(List.of(testReservation));
        
        assertThrows(InsufficientStockException.class, () ->
            productReservationService.reserveProduct(1L, 1L, 9)
        );
        
        verify(productReservationRepository, never()).save(any(ProductReservation.class));
    }
    
    @Test
    void releaseReservation_ShouldDeactivateReservation() {
        when(productReservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        
        productReservationService.releaseReservation(1L);
        
        assertFalse(testReservation.isActive());
        verify(productReservationRepository).save(testReservation);
    }
    
    @Test
    void releaseReservation_ShouldThrowException_WhenReservationNotFound() {
        when(productReservationRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () ->
            productReservationService.releaseReservation(1L)
        );
        
        verify(productReservationRepository, never()).save(any(ProductReservation.class));
    }
    
    @Test
    void releaseReservationsForOrder_ShouldDeactivateReservations() {
        when(productReservationRepository.deactivateReservationsForUser(testProduct, testUser))
            .thenReturn(1);
        
        productReservationService.releaseReservationsForOrder(testProduct, testUser);
        
        verify(productReservationRepository).deactivateReservationsForUser(testProduct, testUser);
    }
    
    @Test
    void getUserReservations_ShouldReturnActiveReservations() {
        when(productReservationRepository.findByUserIdAndActiveTrue(1L))
            .thenReturn(List.of(testReservation));
        
        List<ProductReservation> result = productReservationService.getUserReservations(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertTrue(result.get(0).isActive());
    }
    
    @Test
    void getProductReservations_ShouldReturnActiveReservations() {
        when(productReservationRepository.findByProductIdAndActiveTrue(1L))
            .thenReturn(List.of(testReservation));
        
        List<ProductReservation> result = productReservationService.getProductReservations(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertTrue(result.get(0).isActive());
    }
} 
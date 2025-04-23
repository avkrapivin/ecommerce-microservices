package com.ecommerce.products.service;

import com.ecommerce.products.dto.*;
import com.ecommerce.products.entity.*;
import com.ecommerce.products.repository.*;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceSecurityTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserService userService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_ShouldAllowAdmin() {
        // Given
        CreateProductDto createDto = new CreateProductDto();
        createDto.setName("Test Product");
        createDto.setDescription("Test Description");
        createDto.setPrice(BigDecimal.valueOf(100));
        createDto.setStockQuantity(10);
        createDto.setCategoryId(1L);

        Category category = new Category();
        category.setId(1L);
        category.setName("Test Category");

        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(admin);
        SecurityContextHolder.setContext(securityContext);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(1L);
            product.setCreatedAt(LocalDateTime.now());
            product.setUpdatedAt(LocalDateTime.now());
            return product;
        });

        // When
        ProductDto result = productService.createProduct(createDto);

        // Then
        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void createProduct_ShouldDenyRegularUser() {
        // Given
        CreateProductDto createDto = new CreateProductDto();
        createDto.setName("Test Product");
        createDto.setDescription("Test Description");
        createDto.setPrice(BigDecimal.valueOf(100));
        createDto.setStockQuantity(10);
        createDto.setCategoryId(1L);

        User regularUser = new User();
        regularUser.setId(1L);
        regularUser.setRole(Role.USER);

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(regularUser);
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> productService.createProduct(createDto));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_ShouldAllowAdmin() {
        // Given
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setName("Old Name");
        existingProduct.setCreatedAt(LocalDateTime.now());
        existingProduct.setUpdatedAt(LocalDateTime.now());

        UpdateProductDto updateDto = new UpdateProductDto();
        updateDto.setCategoryId(1L);
        updateDto.setName("New Name");

        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);

        Category category = new Category();
        category.setId(1L);
        category.setName("Test Category");

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(admin);
        SecurityContextHolder.setContext(securityContext);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        // When
        ProductDto result = productService.updateProduct(1L, updateDto);

        // Then
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void updateProduct_ShouldDenyRegularUser() {
        // Given
        UpdateProductDto updateDto = new UpdateProductDto();
        updateDto.setName("New Name");

        User regularUser = new User();
        regularUser.setId(1L);
        regularUser.setRole(Role.USER);

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(regularUser);
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> productService.updateProduct(1L, updateDto));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_ShouldAllowAdmin() {
        // Given
        Product product = new Product();
        product.setId(1L);

        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(admin);
        SecurityContextHolder.setContext(securityContext);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(productRepository).delete(product);

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productRepository, times(1)).delete(product);
    }

    @Test
    void deleteProduct_ShouldDenyRegularUser() {
        // Given
        User regularUser = new User();
        regularUser.setId(1L);
        regularUser.setRole(Role.USER);

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(regularUser);
        SecurityContextHolder.setContext(securityContext);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> productService.deleteProduct(1L));
        verify(productRepository, never()).delete(any(Product.class));
    }
}
package com.ecommerce.products.service;

import com.ecommerce.products.dto.*;
import com.ecommerce.products.entity.*;
import com.ecommerce.products.repository.*;
import com.ecommerce.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceUnitTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductSpecificationRepository productSpecificationRepository;

    @Mock
    private ProductReviewRepository productReviewRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_ShouldCreateProductSuccessfully() {
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
        assertEquals("Test Description", result.getDescription());
        assertEquals(BigDecimal.valueOf(100), result.getPrice());
        assertEquals(10, result.getStockQuantity());
        assertEquals(1L, result.getCategoryId());
        verify(categoryRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void createProduct_ShouldThrowException_WhenCategoryNotFound() {
        // Given
        CreateProductDto createDto = new CreateProductDto();
        createDto.setCategoryId(999L);

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
            productService.createProduct(createDto)
        );
        verify(categoryRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getProductById_ShouldReturnProduct() {
        // Given
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setPrice(BigDecimal.valueOf(100));
        product.setStockQuantity(10);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        Category category = new Category();
        category.setId(1L);
        category.setName("Test Category");
        product.setCategory(category);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        // When
        ProductDto result = productService.getProductById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Product", result.getName());
        assertEquals("Test Description", result.getDescription());
        assertEquals(BigDecimal.valueOf(100), result.getPrice());
        assertEquals(10, result.getStockQuantity());
        assertEquals(1L, result.getCategoryId());
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void getProductById_ShouldThrowException_WhenProductNotFound() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
            productService.getProductById(999L)
        );
        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    void updateProduct_ShouldUpdateProductSuccessfully() {
        // Given
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setName("Old Name");
        existingProduct.setDescription("Old Description");
        existingProduct.setPrice(BigDecimal.valueOf(50));
        existingProduct.setStockQuantity(5);
        existingProduct.setCreatedAt(LocalDateTime.now());
        existingProduct.setUpdatedAt(LocalDateTime.now());

        Category category = new Category();
        category.setId(1L);
        category.setName("Test Category");
        existingProduct.setCategory(category);

        UpdateProductDto updateDto = new UpdateProductDto();
        updateDto.setName("New Name");
        updateDto.setDescription("New Description");
        updateDto.setPrice(BigDecimal.valueOf(100));
        updateDto.setStockQuantity(10);
        updateDto.setCategoryId(1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(existingProduct);

        // When
        ProductDto result = productService.updateProduct(1L, updateDto);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("New Name", result.getName());
        assertEquals("New Description", result.getDescription());
        assertEquals(BigDecimal.valueOf(100), result.getPrice());
        assertEquals(10, result.getStockQuantity());
        assertEquals(1L, result.getCategoryId());
        verify(productRepository, times(1)).findById(1L);
        verify(categoryRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void deleteProduct_ShouldDeleteProductSuccessfully() {
        // Given
        Product product = new Product();
        product.setId(1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(productRepository).delete(product);

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).delete(product);
    }

    @Test
    void deleteProduct_ShouldThrowException_WhenProductNotFound() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
            productService.deleteProduct(999L)
        );
        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).delete(any(Product.class));
    }
} 
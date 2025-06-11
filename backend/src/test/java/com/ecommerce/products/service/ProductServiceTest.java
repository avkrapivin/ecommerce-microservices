package com.ecommerce.products.service;

import com.ecommerce.products.ProductIntegrationTest;
import com.ecommerce.products.dto.CreateProductDto;
import com.ecommerce.products.dto.ProductDto;
import com.ecommerce.products.dto.ProductFilterDto;
import com.ecommerce.products.dto.UpdateProductDto;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.event.OrderEventPublisher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class ProductServiceTest extends ProductIntegrationTest {
    @Autowired
    private ProductService productService;

    @MockBean
    private OrderEventPublisher orderEventPublisher;

    @Test
    void getProductById_ShouldReturnProduct() {
        ProductDto product = productService.getProductById(testProduct.getId());
        
        assertNotNull(product);
        assertEquals(testProduct.getName(), product.getName());
        assertEquals(testProduct.getPrice().doubleValue(), product.getPrice().doubleValue());
    }

    @Test
    void createProduct_ShouldCreateNewProduct() {
        CreateProductDto createProductDto = new CreateProductDto();
        createProductDto.setName("New Product");
        createProductDto.setDescription("New Description");
        createProductDto.setPrice(BigDecimal.valueOf(200.00));
        createProductDto.setStockQuantity(20);
        createProductDto.setCategoryId(testCategory.getId());
        createProductDto.setSku("test-sku-2");

        ProductDto createdProduct = productService.createProduct(createProductDto);

        assertNotNull(createdProduct.getId());
        assertEquals("New Product", createdProduct.getName());
        assertEquals(BigDecimal.valueOf(200.00), createdProduct.getPrice());
    }

    @Test
    void updateProduct_ShouldUpdateProduct() {
        UpdateProductDto updateProductDto = new UpdateProductDto();
        updateProductDto.setName("Updated Product");
        updateProductDto.setDescription("Updated Description");
        updateProductDto.setPrice(BigDecimal.valueOf(150.00));
        updateProductDto.setStockQuantity(15);
        updateProductDto.setCategoryId(testCategory.getId());
        updateProductDto.setActive(true);
        updateProductDto.setSku("test-sku-2");

        ProductDto updatedProduct = productService.updateProduct(testProduct.getId(), updateProductDto);

        assertEquals("Updated Product", updatedProduct.getName());
        assertEquals(BigDecimal.valueOf(150.00), updatedProduct.getPrice());
    }

    @Test
    void deleteProduct_ShouldDeleteProduct() {
        productService.deleteProduct(testProduct.getId());

        assertThrows(ResourceNotFoundException.class, () -> 
            productService.getProductById(testProduct.getId())
        );
    }

    @Test
    void getProducts_ShouldReturnFilteredProducts() {
        ProductFilterDto filter = new ProductFilterDto();
        filter.setCategoryId(testCategory.getId());
        filter.setMinPrice(BigDecimal.valueOf(50));
        filter.setMaxPrice(BigDecimal.valueOf(200));
        filter.setSortBy("price");
        filter.setSortDirection("asc");

        Page<ProductDto> products = productService.getProducts(filter);

        assertNotNull(products);
        assertEquals(1, products.getTotalElements());
        assertEquals(testProduct.getId(), products.getContent().get(0).getId());
    }
} 
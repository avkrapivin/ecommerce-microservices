package com.ecommerce.products.controller;

import com.ecommerce.products.ProductIntegrationTest;
import com.ecommerce.products.dto.CreateProductDto;
import com.ecommerce.products.dto.UpdateProductDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ProductControllerTest extends ProductIntegrationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getProductById_ShouldReturnProduct() throws Exception {
        mockMvc.perform(get("/products/{id}", testProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testProduct.getId()))
                .andExpect(jsonPath("$.name").value(testProduct.getName()))
                .andExpect(jsonPath("$.price").value(testProduct.getPrice().doubleValue()));
    }

    @Test
    void createProduct_ShouldCreateNewProduct() throws Exception {
        CreateProductDto createProductDto = new CreateProductDto();
        createProductDto.setName("New Product");
        createProductDto.setDescription("New Description");
        createProductDto.setPrice(BigDecimal.valueOf(200.00));
        createProductDto.setStockQuantity(20);
        createProductDto.setCategoryId(testCategory.getId());

        MvcResult result = mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createProductDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertNotNull(location);
        assertTrue(location.contains("/products/"));
    }

    @Test
    void updateProduct_ShouldUpdateProduct() throws Exception {
        UpdateProductDto updateProductDto = new UpdateProductDto();
        updateProductDto.setName("Updated Product");
        updateProductDto.setDescription("Updated Description");
        updateProductDto.setPrice(BigDecimal.valueOf(150.00));
        updateProductDto.setStockQuantity(15);
        updateProductDto.setCategoryId(testCategory.getId());
        updateProductDto.setActive(true);

        mockMvc.perform(put("/products/{id}", testProduct.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProductDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Product"))
                .andExpect(jsonPath("$.price").value(150.00));
    }

    @Test
    void deleteProduct_ShouldDeleteProduct() throws Exception {
        mockMvc.perform(delete("/products/{id}", testProduct.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products/{id}", testProduct.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProducts_ShouldReturnFilteredProducts() throws Exception {
        mockMvc.perform(get("/products")
                .param("categoryId", testCategory.getId().toString())
                .param("minPrice", "50")
                .param("maxPrice", "200")
                .param("sortBy", "price")
                .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(testProduct.getId()));
    }
} 
package com.ecommerce.products;

import com.ecommerce.order.event.OrderEventPublisher;
import com.ecommerce.products.entity.Category;
import com.ecommerce.products.entity.Product;
import com.ecommerce.products.repository.CategoryRepository;
import com.ecommerce.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProductIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected CategoryRepository categoryRepository;

    protected Category testCategory;
    protected Product testProduct;

    @BeforeEach
    void setUp() {
        // Очищаем базу данных перед каждым тестом
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Создаем тестовую категорию
        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setDescription("Test Description");
        testCategory = categoryRepository.save(testCategory);

        // Создаем тестовый продукт
        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(BigDecimal.valueOf(100.00));
        testProduct.setStockQuantity(10);
        testProduct.setCategory(testCategory);
        testProduct.setActive(true);
        testProduct.setSku("test-sku");
        testProduct = productRepository.save(testProduct);
    }
} 
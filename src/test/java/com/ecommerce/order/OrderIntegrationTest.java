package com.ecommerce.order;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentStatus;
import com.ecommerce.order.entity.ShippingAddress;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OrderItemRepository;
import com.ecommerce.products.entity.Category;
import com.ecommerce.products.entity.Product;
import com.ecommerce.products.repository.CategoryRepository;
import com.ecommerce.products.repository.ProductRepository;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OrderIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected OrderItemRepository orderItemRepository;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected CategoryRepository categoryRepository;

    @Autowired
    protected UserRepository userRepository;

    protected User testUser;
    protected Product testProduct;
    protected Order testOrder;
    protected OrderItem testOrderItem;
    protected Category testCategory;
    protected ShippingAddress testShippingAddress;

    @BeforeEach
    void setUp() {
        // Очищаем базу данных перед каждым тестом
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        // Создаем тестовую категорию
        testCategory = new Category();
        testCategory.setName("Test Category");
        testCategory.setDescription("Test Description");
        testCategory = categoryRepository.save(testCategory);

        // Создаем тестового пользователя
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setCognitoId("test-cognito-id");
        testUser.setId(1L);
        testUser = userRepository.save(testUser);

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

        // Создаем тестовый адрес доставки
        testShippingAddress = new ShippingAddress();
        testShippingAddress.setFirstName("John");
        testShippingAddress.setLastName("Doe");
        testShippingAddress.setStreet("123 Main St");
        testShippingAddress.setCity("New York");
        testShippingAddress.setState("NY");
        testShippingAddress.setPostalCode("10001");
        testShippingAddress.setCountry("USA");
        testShippingAddress.setPhoneNumber("+1234567890");
        testShippingAddress.setEmail("john.doe@example.com");

        // Создаем тестовый заказ
        testOrder = new Order();
        testOrder.setUser(testUser);
        testOrder.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8));
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setPaymentStatus(PaymentStatus.PENDING);
        testOrder.setShippingAddress(testShippingAddress);

        // Создаем тестовый элемент заказа
        testOrderItem = new OrderItem();
        testOrderItem.setOrder(testOrder);
        testOrderItem.setProduct(testProduct);
        testOrderItem.setQuantity(1);
        testOrderItem.setUnitPrice(testProduct.getPrice());
        testOrderItem.setTotalPrice(testProduct.getPrice());
        //testOrderItem = orderItemRepository.save(testOrderItem);

        testOrder.getItems().add(testOrderItem);
        testOrder.setSubtotal(BigDecimal.valueOf(100.00));
        testOrder.setShippingCost(BigDecimal.ZERO);
        testOrder.setTax(BigDecimal.ZERO);
        testOrder.setTotal(BigDecimal.valueOf(100.00));
        testOrder = orderRepository.save(testOrder);

    }
}
package com.ecommerce.order.service;

import com.ecommerce.common.exception.OrderStatusException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.OrderIntegrationTest;
import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.*;
import com.ecommerce.order.repository.OrderItemRepository;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.products.entity.Product;
import com.ecommerce.products.service.ProductService;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
        org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
public class OrderServiceTest extends OrderIntegrationTest {

    @Autowired
    private OrderService orderService;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private UserService userService;

    @MockBean
    private ProductService productService;

    @MockBean
    private OrderCalculationService calculationService;

    private Order testOrder;
    private User testUser;
    private ShippingAddress testShippingAddress;

    @BeforeEach
    void setUpTestData() {
        testUser = new User();
        testUser.setId(1L);

        testShippingAddress = new ShippingAddress();
        testShippingAddress.setId(1L);
        testShippingAddress.setFirstName("John");
        testShippingAddress.setLastName("Doe");
        testShippingAddress.setStreet("123 Main St");
        testShippingAddress.setCity("New York");
        testShippingAddress.setState("NY");
        testShippingAddress.setPostalCode("10001");
        testShippingAddress.setCountry("USA");
        testShippingAddress.setPhoneNumber("+1234567890");
        testShippingAddress.setEmail("john.doe@example.com");

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setShippingAddress(testShippingAddress);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setPaymentStatus(PaymentStatus.PENDING);
        testOrder.setOrderNumber("ORD-123456");
        testOrder.setSubtotal(BigDecimal.valueOf(100.0));
        testOrder.setShippingCost(BigDecimal.ZERO);
        testOrder.setTax(BigDecimal.ZERO);
        testOrder.setTotal(BigDecimal.valueOf(100.0));
    }

    private OrderItemRequest createOrderItemRequest(Long productId, int quantity) {
        OrderItemRequest itemRequest = new OrderItemRequest();
        ProductRequest productRequest = new ProductRequest();
        productRequest.setId(productId);
        itemRequest.setProduct(productRequest);
        itemRequest.setQuantity(quantity);
        return itemRequest;
    }

    private ShippingAddressRequest createShippingAddressRequest() {
        ShippingAddressRequest shippingAddress = new ShippingAddressRequest();
        shippingAddress.setFirstName("John");
        shippingAddress.setLastName("Doe");
        shippingAddress.setStreet("123 Main St");
        shippingAddress.setCity("New York");
        shippingAddress.setState("NY");
        shippingAddress.setPostalCode("10001");
        shippingAddress.setCountry("USA");
        shippingAddress.setPhoneNumber("+1234567890");
        shippingAddress.setEmail("john.doe@example.com");
        return shippingAddress;
    }

    @Test
    void createOrder_ShouldReturnCreatedOrder() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        Product product = new Product();
        product.setId(1L);
        product.setPrice(BigDecimal.valueOf(50.0));

        OrderRequest request = new OrderRequest();
        request.setItems(List.of(createOrderItemRequest(product.getId(), 2)));
        request.setShippingAddress(createShippingAddressRequest());

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setUser(user);
        savedOrder.setShippingAddress(testShippingAddress);
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setPaymentStatus(PaymentStatus.PENDING);

        OrderItem saveOrderItem = new OrderItem();
        saveOrderItem.setOrder(savedOrder);
        saveOrderItem.setProduct(product);
        saveOrderItem.setQuantity(1);
        saveOrderItem.setUnitPrice(product.getPrice());
        saveOrderItem.setTotalPrice(product.getPrice());

        savedOrder.getItems().add(saveOrderItem);
        savedOrder.setSubtotal(BigDecimal.valueOf(100.0));
        savedOrder.setShippingCost(BigDecimal.ZERO);
        savedOrder.setTax(BigDecimal.ZERO);
        savedOrder.setTotal(BigDecimal.valueOf(100.0));

        when(userService.getUserById(userId)).thenReturn(user);
        when(productService.getProductEntityById(any())).thenReturn(product);
        when(orderRepository.save(any())).thenReturn(savedOrder);
        when(calculationService.calculateShippingCost(any())).thenReturn(BigDecimal.ZERO);
        when(calculationService.calculateTax(any())).thenReturn(BigDecimal.ZERO);
        when(calculationService.calculateTotal(any(), any(), any())).thenReturn(BigDecimal.valueOf(100.0));

        // Act
        OrderDto response = orderService.createOrder(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(savedOrder.getId(), response.getId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
        assertEquals(BigDecimal.valueOf(100.0), response.getTotal());
        assertEquals(1, response.getItems().size());
        assertNotNull(response.getShippingAddress());

        verify(userService).getUserById(userId);
        verify(productService).getProductEntityById(product.getId());
        verify(calculationService).calculateShippingCost(any());
        verify(calculationService).calculateTax(any());
        verify(calculationService).calculateTotal(any(), any(), any());
    }

    @Test
    void getOrderById_ShouldReturnOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        OrderDto result = orderService.getOrderById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ORD-123456", result.getOrderNumber());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(PaymentStatus.PENDING, result.getPaymentStatus());
    }

    @Test
    void getOrderById_ShouldThrowException_WhenOrderNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(1L));
    }

    @Test
    void getOrderByNumber_ShouldReturnOrder() {
        when(orderRepository.findByOrderNumber("ORD-123456")).thenReturn(Optional.of(testOrder));

        OrderDto result = orderService.getOrderByNumber("ORD-123456");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ORD-123456", result.getOrderNumber());
    }

    @Test
    void getOrderByNumber_ShouldThrowException_WhenOrderNotFound() {
        when(orderRepository.findByOrderNumber("ORD-123456")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderByNumber("ORD-123456"));
    }

    @Test
    void getUserOrders_ShouldReturnUserOrders() {
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(orderRepository.findByUser(testUser)).thenReturn(List.of(testOrder));

        List<OrderDto> result = orderService.getUserOrders(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void createOrder_ShouldThrowException_WhenItemsEmpty() {
        OrderRequest request = new OrderRequest();
        request.setItems(List.of());

        when(userService.getUserById(1L)).thenReturn(testUser);

        assertThrows(OrderStatusException.class, () -> orderService.createOrder(1L, request));
    }

    @Test
    void updateOrderStatus_ShouldUpdateOrderStatus() {
        UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto();
        updateDto.setStatus(OrderStatus.PROCESSING);
        updateDto.setPaymentStatus(PaymentStatus.COMPLETED);
        updateDto.setTrackingNumber("TRACK-123456");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any())).thenReturn(testOrder);

        OrderDto result = orderService.updateOrderStatus(1L, updateDto);

        assertNotNull(result);
        assertEquals(OrderStatus.PROCESSING, result.getStatus());
        assertEquals(PaymentStatus.COMPLETED, result.getPaymentStatus());
        assertEquals("TRACK-123456", result.getTrackingNumber());
        
        verify(orderRepository, atLeastOnce()).save(any());
    }

    @Test
    void updateOrderStatus_ShouldThrowException_WhenOrderNotFound() {
        UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto();
        updateDto.setStatus(OrderStatus.PROCESSING);
        updateDto.setPaymentStatus(PaymentStatus.COMPLETED);
        updateDto.setTrackingNumber("TRACK-123456");

        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.updateOrderStatus(1L, updateDto));
    }

    @Test
    void cancelOrder_ShouldCancelOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any())).thenReturn(testOrder);

        orderService.cancelOrder(1L);

        verify(orderRepository, atLeastOnce()).save(any());
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
    }

    @Test
    void cancelOrder_ShouldThrowException_WhenOrderNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.cancelOrder(1L));
    }

    @Test
    void cancelOrder_ShouldThrowException_WhenOrderNotPending() {
        testOrder.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThrows(OrderStatusException.class, () -> orderService.cancelOrder(1L));
    }
}
package com.ecommerce.order.service;

import com.ecommerce.common.exception.OrderStatusException;
import com.ecommerce.common.exception.ResourceNotFoundException;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceUnitTest {
    @Mock 
    private OrderRepository orderRepository;
    
    @Mock
    private OrderItemRepository orderItemRepository;
    
    @Mock
    private UserService userService;
    
    @Mock
    private ProductService productService;
    
    @Mock
    private OrderCalculationService calculationService;
    
    @InjectMocks
    private OrderService orderService;
    
    private User testUser;
    private Product testProduct;
    private Order testOrder;
    private OrderItem testOrderItem;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        ShippingAddress testShippingAddress = new ShippingAddress();
        testShippingAddress.setFirstName("John");
        testShippingAddress.setLastName("Doe");
        testShippingAddress.setStreet("123 Main St");
        testShippingAddress.setCity("New York");
        testShippingAddress.setState("NY");
        testShippingAddress.setPostalCode("10001");
        testShippingAddress.setCountry("USA");
        testShippingAddress.setPhoneNumber("+1234567890");
        testShippingAddress.setEmail("john.doe@example.com");
        
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(BigDecimal.valueOf(100.00));
        
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setShippingAddress(testShippingAddress);
        testOrder.setOrderNumber("ORD-123456");
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setPaymentStatus(PaymentStatus.PENDING);
        
        testOrderItem = new OrderItem();
        testOrderItem.setId(1L);
        testOrderItem.setOrder(testOrder);
        testOrderItem.setProduct(testProduct);
        testOrderItem.setQuantity(1);
        testOrderItem.setUnitPrice(testProduct.getPrice());
        
        testOrder.setItems(List.of(testOrderItem));
    }
    
    @Test
    void getOrderById_ShouldReturnOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        
        OrderDto result = orderService.getOrderById(1L);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ORD-123456", result.getOrderNumber());
        assertEquals(OrderStatus.PENDING, result.getStatus());
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
    void createOrder_ShouldCreateNewOrder() {
        OrderRequest request = new OrderRequest();
        
        OrderItemRequest itemRequest = new OrderItemRequest();
        ProductRequest productRequest = new ProductRequest();
        productRequest.setId(1L);
        itemRequest.setProduct(productRequest);
        itemRequest.setQuantity(1);
        
        request.setItems(List.of(itemRequest));
        
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
        
        request.setShippingAddress(shippingAddress);
        
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(productService.getProductEntityById(1L)).thenReturn(testProduct);
        when(calculationService.calculateShippingCost(any())).thenReturn(BigDecimal.ZERO);
        when(calculationService.calculateTax(any())).thenReturn(BigDecimal.ZERO);
        when(calculationService.calculateTotal(any(), any(), any())).thenReturn(BigDecimal.valueOf(100.00));
        when(orderRepository.save(any())).thenReturn(testOrder);
        
        OrderDto result = orderService.createOrder(1L, request);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(orderRepository, atLeastOnce()).save(any());
    }
    
    @Test
    void createOrder_ShouldThrowException_WhenItemsEmpty() {
        OrderRequest request = new OrderRequest();
        request.setItems(List.of());
        
        lenient().when(userService.getUserById(1L)).thenReturn(testUser);
        
        assertThrows(OrderStatusException.class, () -> orderService.createOrder(1L, request));
    }
    
    @Test
    void updateOrderStatus_ShouldUpdateOrderStatus() {
        UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto();
        updateDto.setStatus(OrderStatus.PROCESSING);
        
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any())).thenReturn(testOrder);
        
        OrderDto result = orderService.updateOrderStatus(1L, updateDto);
        
        assertNotNull(result);
        assertEquals(OrderStatus.PROCESSING, result.getStatus());
        verify(orderRepository).save(any());
    }
    
    @Test
    void updateOrderStatus_ShouldThrowException_WhenOrderNotFound() {
        UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto();
        updateDto.setStatus(OrderStatus.PROCESSING);
        
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(ResourceNotFoundException.class, () -> orderService.updateOrderStatus(1L, updateDto));
    }
    
    @Test
    void cancelOrder_ShouldCancelOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any())).thenReturn(testOrder);
        
        orderService.cancelOrder(1L);
        
        verify(orderRepository).save(any());
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
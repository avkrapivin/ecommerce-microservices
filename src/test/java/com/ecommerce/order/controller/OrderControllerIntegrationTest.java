package com.ecommerce.order.controller;

import com.ecommerce.order.OrderIntegrationTest;
import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentStatus;
import com.ecommerce.order.service.OrderCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
public class OrderControllerIntegrationTest extends OrderIntegrationTest {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private OrderCalculationService calculationService;

    
    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void createOrder_ShouldReturnCreatedOrder() throws Exception {
        // Настраиваем моки для расчета стоимости
        when(calculationService.calculateShippingCost(any())).thenReturn(BigDecimal.ZERO);
        when(calculationService.calculateTax(any())).thenReturn(BigDecimal.ZERO);
        when(calculationService.calculateTotal(any(), any(), any())).thenReturn(BigDecimal.valueOf(100.00));

        OrderRequest request = new OrderRequest();
        
        OrderItemRequest itemRequest = new OrderItemRequest();
        ProductRequest productRequest = new ProductRequest();
        productRequest.setId(testProduct.getId());
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

        mockMvc.perform(post("/orders/user/{userId}", testUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value(OrderStatus.PENDING.toString()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.PENDING.toString()))
                .andExpect(jsonPath("$.total").value(100.00));
    }
    
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateOrderStatus_ShouldReturnUpdatedOrder() throws Exception {
        UpdateOrderStatusDto updateDto = new UpdateOrderStatusDto();
        updateDto.setStatus(OrderStatus.PROCESSING);
        updateDto.setPaymentStatus(PaymentStatus.COMPLETED);
        updateDto.setTrackingNumber("TRACK-123456");

        mockMvc.perform(put("/orders/{id}/status", testOrder.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrder.getId()))
                .andExpect(jsonPath("$.status").value(OrderStatus.PROCESSING.toString()))
                .andExpect(jsonPath("$.paymentStatus").value(PaymentStatus.COMPLETED.toString()))
                .andExpect(jsonPath("$.trackingNumber").value("TRACK-123456"));
    }
    
    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getOrderById_ShouldReturnOrder() throws Exception {
        mockMvc.perform(get("/orders/{id}", testOrder.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testOrder.getId()))
                .andExpect(jsonPath("$.status").value(testOrder.getStatus().toString()))
                .andExpect(jsonPath("$.paymentStatus").value(testOrder.getPaymentStatus().toString()));
    }
    
    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getUserOrders_ShouldReturnUserOrders() throws Exception {
        mockMvc.perform(get("/orders/user/{userId}", testUser.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(testOrder.getId()))
                .andExpect(jsonPath("$[0].status").value(testOrder.getStatus().toString()))
                .andExpect(jsonPath("$[0].paymentStatus").value(testOrder.getPaymentStatus().toString()));
    }
} 
package com.ecommerce.shipping.controller;

import com.ecommerce.shipping.dto.ShippingInfoDto;
import com.ecommerce.shipping.exception.ShippingInfoNotFoundException;
import com.ecommerce.shipping.model.ShippingStatus;
import com.ecommerce.shipping.service.ShippingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class ShippingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShippingService shippingService;

    @Test
    @WithMockUser
    void getShippingInfo_ShouldReturnShippingInfo() throws Exception {
        // Arrange
        ShippingInfoDto shippingInfo = new ShippingInfoDto();
        shippingInfo.setId(1L);
        shippingInfo.setOrderId("order_123");
        shippingInfo.setTrackingNumber("1Z999AA10123456789");
        shippingInfo.setStatus(ShippingStatus.LABEL_CREATED);
        shippingInfo.setCreatedAt(LocalDateTime.now());
        shippingInfo.setUpdatedAt(LocalDateTime.now());

        when(shippingService.getShippingInfo("order_123")).thenReturn(shippingInfo);

        // Act & Assert
        mockMvc.perform(get("/shipping/order_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value("order_123"))
                .andExpect(jsonPath("$.trackingNumber").value("1Z999AA10123456789"))
                .andExpect(jsonPath("$.status").value("LABEL_CREATED"));
    }

    @Test
    @WithMockUser
    void getShippingInfo_WhenNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(shippingService.getShippingInfo("order_123")).thenThrow(new ShippingInfoNotFoundException("Shipping info not found for order: order_123"));

        // Act & Assert
        mockMvc.perform(get("/shipping/order_123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Shipping info not found for order: order_123"));
    }

    @Test
    @WithMockUser
    void getShippingInfoByTracking_ShouldReturnShippingInfo() throws Exception {
        // Arrange
        ShippingInfoDto shippingInfo = new ShippingInfoDto();
        shippingInfo.setId(1L);
        shippingInfo.setOrderId("order_123");
        shippingInfo.setTrackingNumber("1Z999AA10123456789");
        shippingInfo.setStatus(ShippingStatus.IN_TRANSIT);
        shippingInfo.setCreatedAt(LocalDateTime.now());
        shippingInfo.setUpdatedAt(LocalDateTime.now());

        when(shippingService.getShippingInfoByTracking("1Z999AA10123456789")).thenReturn(shippingInfo);

        // Act & Assert
        mockMvc.perform(get("/shipping/tracking/1Z999AA10123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value("order_123"))
                .andExpect(jsonPath("$.trackingNumber").value("1Z999AA10123456789"))
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    }

    @Test
    @WithMockUser
    void getShippingInfoByTracking_WhenNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(shippingService.getShippingInfoByTracking("1Z999AA10123456789")).thenThrow(new ShippingInfoNotFoundException("Shipping info not found for tracking: 1Z999AA10123456789"));

        // Act & Assert
        mockMvc.perform(get("/shipping/tracking/1Z999AA10123456789"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Shipping info not found for tracking: 1Z999AA10123456789"));
    }
} 
package com.ecommerce.shipping.controller;

import com.ecommerce.shipping.dto.*;
import com.ecommerce.shipping.exception.ShippingInfoNotFoundException;
import com.ecommerce.shipping.model.ShippingStatus;
import com.ecommerce.shipping.service.ShippingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

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
    void calculateShippingRates_ShouldReturnRates() throws Exception {
        // Arrange
        ShippingRateResponseDto response = new ShippingRateResponseDto();
        ShippingRateResponseDto.ShippingRateDto rate = new ShippingRateResponseDto.ShippingRateDto();
        rate.setObjectId("rate_123");
        rate.setProvider("USPS");
        rate.setService("Priority");
        rate.setCurrency("USD");
        rate.setAmount("10.00");
        rate.setDays("3");
        rate.setEstimatedDays("3");
        rate.setDurationTerms("3-5 business days");
        rate.setProviderImage75("https://example.com/usps_75.png");
        rate.setProviderImage200("https://example.com/usps_200.png");
        response.setRates(List.of(rate));

        when(shippingService.calculateShippingRates(any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/shipping/rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAddress\":{\"name\":\"John Doe\",\"street1\":\"123 Main St\",\"city\":\"New York\",\"state\":\"NY\",\"zip\":\"10001\",\"country\":\"US\",\"phone\":\"+1234567890\",\"email\":\"john@example.com\"},\"toAddress\":{\"name\":\"Jane Smith\",\"street1\":\"456 Oak St\",\"city\":\"Los Angeles\",\"state\":\"CA\",\"zip\":\"90001\",\"country\":\"US\",\"phone\":\"+1987654321\",\"email\":\"jane@example.com\"},\"parcels\":[{\"length\":\"5\",\"width\":\"5\",\"height\":\"5\",\"distance_unit\":\"in\",\"weight\":\"2\",\"mass_unit\":\"lb\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rates[0].objectId").value("rate_123"))
                .andExpect(jsonPath("$.rates[0].provider").value("USPS"))
                .andExpect(jsonPath("$.rates[0].service").value("Priority"))
                .andExpect(jsonPath("$.rates[0].currency").value("USD"))
                .andExpect(jsonPath("$.rates[0].amount").value("10.00"))
                .andExpect(jsonPath("$.rates[0].days").value("3"))
                .andExpect(jsonPath("$.rates[0].estimatedDays").value("3"))
                .andExpect(jsonPath("$.rates[0].durationTerms").value("3-5 business days"));
    }

    @Test
    @WithMockUser
    void calculateShippingRates_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        when(shippingService.calculateShippingRates(any())).thenThrow(new IOException("Failed to connect to shipping provider"));

        // Act & Assert
        mockMvc.perform(post("/shipping/rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromAddress\":{\"name\":\"John Doe\",\"street1\":\"123 Main St\",\"city\":\"New York\",\"state\":\"NY\",\"zip\":\"10001\",\"country\":\"US\",\"phone\":\"+1234567890\",\"email\":\"john@example.com\"},\"toAddress\":{\"name\":\"Jane Smith\",\"street1\":\"456 Oak St\",\"city\":\"Los Angeles\",\"state\":\"CA\",\"zip\":\"90001\",\"country\":\"US\",\"phone\":\"+1987654321\",\"email\":\"jane@example.com\"},\"parcels\":[{\"length\":\"5\",\"width\":\"5\",\"height\":\"5\",\"distance_unit\":\"in\",\"weight\":\"2\",\"mass_unit\":\"lb\"}]}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Failed to process shipping request: Failed to connect to shipping provider"));
    }

    @Test
    @WithMockUser
    void generateShippingLabel_ShouldReturnLabel() throws Exception {
        // Arrange
        ShippingLabelResponseDto response = new ShippingLabelResponseDto();
        response.setObjectId("tr_123");
        response.setStatus("SUCCESS");
        response.setMessage("Label generated successfully");
        response.setLabelUrl("https://api.goshippo.com/labels/123.pdf");
        response.setTrackingNumber("1Z999AA10123456789");
        response.setTrackingUrlProvider("https://www.ups.com/track?tracknum=1Z999AA10123456789");
        response.setLabelFileType("PDF");
        response.setLabelSize("4x6");
        response.setLabelResolution("300");
        response.setLabelFileSize("1024");

        when(shippingService.generateShippingLabel(any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/shipping/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"order_123\",\"rateId\":\"rate_123\",\"labelFileType\":\"PDF\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.objectId").value("tr_123"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Label generated successfully"))
                .andExpect(jsonPath("$.labelUrl").value("https://api.goshippo.com/labels/123.pdf"))
                .andExpect(jsonPath("$.trackingNumber").value("1Z999AA10123456789"))
                .andExpect(jsonPath("$.trackingUrlProvider").value("https://www.ups.com/track?tracknum=1Z999AA10123456789"));
    }

    @Test
    @WithMockUser
    void generateShippingLabel_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        // Arrange
        when(shippingService.generateShippingLabel(any())).thenThrow(new IOException("Failed to generate shipping label"));

        // Act & Assert
        mockMvc.perform(post("/shipping/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"order_123\",\"rateId\":\"rate_123\",\"labelFileType\":\"PDF\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Failed to process shipping request: Failed to generate shipping label"));
    }

    @Test
    @WithMockUser
    void getShippingLabel_ShouldReturnShippingInfo() throws Exception {
        // Arrange
        ShippingInfoDto shippingInfo = new ShippingInfoDto();
        shippingInfo.setId(1L);
        shippingInfo.setOrderId("order_123");
        shippingInfo.setTrackingNumber("1Z999AA10123456789");
        shippingInfo.setStatus(ShippingStatus.LABEL_CREATED);

        when(shippingService.getShippingInfo("order_123")).thenReturn(shippingInfo);

        // Act & Assert
        mockMvc.perform(get("/shipping/labels/order_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value("order_123"))
                .andExpect(jsonPath("$.trackingNumber").value("1Z999AA10123456789"))
                .andExpect(jsonPath("$.status").value("LABEL_CREATED"));
    }

    @Test
    @WithMockUser
    void getShippingLabel_WhenNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(shippingService.getShippingInfo("order_123")).thenThrow(new ShippingInfoNotFoundException("Shipping info not found for order: order_123"));

        // Act & Assert
        mockMvc.perform(get("/shipping/labels/order_123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Shipping info not found for order: order_123"));
    }

    @Test
    @WithMockUser
    void updateShippingStatus_ShouldReturnOk() throws Exception {
        // Act & Assert
        mockMvc.perform(patch("/shipping/status/order_123")
                .param("status", "IN_TRANSIT"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void updateShippingStatus_WhenNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        doThrow(new ShippingInfoNotFoundException("Shipping info not found for order: order_123"))
                .when(shippingService).updateShippingStatus("order_123", ShippingStatus.IN_TRANSIT);

        // Act & Assert
        mockMvc.perform(patch("/shipping/status/order_123")
                .param("status", "IN_TRANSIT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Shipping info not found for order: order_123"));
    }
} 
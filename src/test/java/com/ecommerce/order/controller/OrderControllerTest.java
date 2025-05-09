package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.UpdateOrderStatusDto;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.payment.dto.PayPalPaymentResponse;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void createPayment_Success() throws Exception {
        // Arrange
        Payment payment = new Payment();
        payment.setId("PAY-123");
        payment.setState("created");
        
        PayPalPaymentResponse response = PayPalPaymentResponse.success(payment);
        when(orderService.createPayment(eq(1L))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/orders/1/payment")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("PAY-123"))
                .andExpect(jsonPath("$.status").value("created"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void createPayment_PayPalError() throws Exception {
        // Arrange
        when(orderService.createPayment(eq(1L))).thenThrow(new PayPalRESTException("Payment error"));

        // Act & Assert
        mockMvc.perform(post("/orders/1/payment")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.errorMessage").value("Payment error"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void handlePaymentSuccess_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/orders/payment/success")
                .param("paymentId", "PAY-123")
                .param("PayerID", "PAYER-123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(orderService).processSuccessfulPayment("PAY-123", "PAYER-123");
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void handlePaymentSuccess_PayPalError() throws Exception {
        // Arrange
        doThrow(new PayPalRESTException("Payment error"))
                .when(orderService).processSuccessfulPayment(eq("PAY-123"), eq("PAYER-123"));

        // Act & Assert
        mockMvc.perform(get("/orders/payment/success")
                .param("paymentId", "PAY-123")
                .param("PayerID", "PAYER-123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(orderService).handlePaymentFailure("PAY-123", "Payment error");
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void handlePaymentCancellation_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/orders/payment/cancel")
                .param("paymentId", "PAY-123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(orderService).handlePaymentCancellation("PAY-123");
    }
} 
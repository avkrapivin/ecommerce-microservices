package com.ecommerce.shipping.controller;

import com.ecommerce.shipping.event.OrderStatusUpdateListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SnsWebhookControllerTest {

    @Mock
    private OrderStatusUpdateListener orderStatusUpdateListener;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SnsWebhookController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new SnsWebhookController(orderStatusUpdateListener, new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void handleSnsWebhook_ShouldProcessNotification() throws Exception {
        // Given
        String payload = "{\"Type\":\"Notification\",\"Message\":\"{\\\"orderId\\\":\\\"ORD-12345678\\\",\\\"status\\\":\\\"SHIPPED\\\"}\",\"Subject\":\"OrderStatusUpdated\"}";

        // When & Then
        mockMvc.perform(post("/delivery/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("x-amz-sns-message-type", "Notification"))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification processed"));
    }

    @Test
    void handleSnsWebhook_ShouldHandleSubscriptionConfirmation() throws Exception {
        // Given
        String payload = "{\"Type\":\"SubscriptionConfirmation\",\"SubscribeURL\":\"https://example.com/confirm\"}";

        // When & Then
        mockMvc.perform(post("/delivery/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("x-amz-sns-message-type", "SubscriptionConfirmation"))
                .andExpect(status().isOk())
                .andExpect(content().string("Subscription confirmed"));
    }

    @Test
    void handleSnsWebhook_ShouldHandleUnknownMessageType() throws Exception {
        // Given
        String payload = "{\"Type\":\"Unknown\"}";

        // When & Then
        mockMvc.perform(post("/delivery/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("x-amz-sns-message-type", "Unknown"))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook received"));
    }

    @Test
    void handleSnsWebhook_ShouldHandleError() throws Exception {
        // Given
        String payload = "invalid json";

        // When & Then
        mockMvc.perform(post("/delivery/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("x-amz-sns-message-type", "Notification"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error processing webhook"));
    }
} 
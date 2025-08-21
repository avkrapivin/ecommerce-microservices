package com.ecommerce.shipping.controller;

import com.ecommerce.shipping.model.ShippingInfo;
import com.ecommerce.shipping.model.ShippingStatus;
import com.ecommerce.shipping.repository.ShippingInfoRepository;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShippoWebhookControllerTest {

    @Mock
    private ShippingInfoRepository shippingInfoRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ShippoWebhookController controller;

    private MockMvc mockMvc;
    private ShippingInfo shippingInfo;

    @BeforeEach
    void setUp() {
        controller = new ShippoWebhookController(shippingInfoRepository, new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        
        shippingInfo = new ShippingInfo();
        shippingInfo.setId(1L);
        shippingInfo.setOrderId(12345678L);
        shippingInfo.setTrackingNumber("TRK123");
        shippingInfo.setStatus(ShippingStatus.PENDING);
        shippingInfo.setCreatedAt(LocalDateTime.now());
        shippingInfo.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void handleShippoWebhook_ShouldUpdateShippingStatus() throws Exception {
        // Given
        String payload = "{\"tracking_number\":\"TRK123\",\"tracking_status\":{\"status\":\"DELIVERED\"},\"shipment_id\":\"ship_123\"}";

        when(shippingInfoRepository.findByTrackingNumberOrShippoShipmentId("TRK123", "ship_123"))
                .thenReturn(Optional.of(shippingInfo));
        when(shippingInfoRepository.save(any(ShippingInfo.class)))
                .thenReturn(shippingInfo);

        // When & Then
        mockMvc.perform(post("/delivery/shippo-webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed successfully"));

        verify(shippingInfoRepository).findByTrackingNumberOrShippoShipmentId("TRK123", "ship_123");
        verify(shippingInfoRepository).save(any(ShippingInfo.class));
    }

    @Test
    void handleShippoWebhook_ShouldHandleMissingShippingInfo() throws Exception {
        // Given
        String payload = "{\"tracking_number\":\"TRK123\",\"tracking_status\":{\"status\":\"DELIVERED\"},\"shipment_id\":\"ship_123\"}";

        when(shippingInfoRepository.findByTrackingNumberOrShippoShipmentId("TRK123", "ship_123"))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/delivery/shippo-webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook received but no matching shipping info found"));

        verify(shippingInfoRepository).findByTrackingNumberOrShippoShipmentId("TRK123", "ship_123");
        verify(shippingInfoRepository, never()).save(any(ShippingInfo.class));
    }

    @Test
    void handleShippoWebhook_ShouldHandleError() throws Exception {
        // Given
        String payload = "invalid json";

        // When & Then
        mockMvc.perform(post("/delivery/shippo-webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error processing webhook"));
    }
} 
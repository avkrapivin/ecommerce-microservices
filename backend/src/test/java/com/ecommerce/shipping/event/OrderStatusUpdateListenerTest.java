package com.ecommerce.shipping.event;

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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderStatusUpdateListenerTest {

    @Mock
    private ShippingInfoRepository shippingInfoRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderStatusUpdateListener listener;

    private ShippingInfo shippingInfo;

    @BeforeEach
    void setUp() {
        shippingInfo = new ShippingInfo();
        shippingInfo.setId(1L);
        shippingInfo.setOrderId("ORD-12345678");
        shippingInfo.setStatus(ShippingStatus.PENDING);
        shippingInfo.setCreatedAt(LocalDateTime.now());
        shippingInfo.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void handleOrderStatusUpdate_ShouldUpdateShippingStatus() throws Exception {
        // Given
        String messageBody = "{\"orderId\":\"ORD-12345678\",\"status\":\"SHIPPED\",\"trackingNumber\":\"TRK123\"}";
        
        when(shippingInfoRepository.findByOrderId("ORD-12345678"))
                .thenReturn(Optional.of(shippingInfo));
        when(shippingInfoRepository.save(any(ShippingInfo.class)))
                .thenReturn(shippingInfo);

        // When
        listener.handleOrderStatusUpdate(messageBody);

        // Then
        verify(shippingInfoRepository).findByOrderId("ORD-12345678");
        verify(shippingInfoRepository).save(any(ShippingInfo.class));
    }

    @Test
    void handleOrderStatusUpdate_ShouldHandleMissingShippingInfo() throws Exception {
        // Given
        String messageBody = "{\"orderId\":\"ORD-12345678\",\"status\":\"SHIPPED\"}";
        
        when(shippingInfoRepository.findByOrderId("ORD-12345678"))
                .thenReturn(Optional.empty());

        // When
        listener.handleOrderStatusUpdate(messageBody);

        // Then
        verify(shippingInfoRepository).findByOrderId("ORD-12345678");
        verify(shippingInfoRepository, never()).save(any(ShippingInfo.class));
    }

    @Test
    void handleOrderStatusUpdate_ShouldHandleUnknownStatus() throws Exception {
        // Given
        String messageBody = "{\"orderId\":\"ORD-12345678\",\"status\":\"UNKNOWN_STATUS\"}";
        
        when(shippingInfoRepository.findByOrderId("ORD-12345678"))
                .thenReturn(Optional.of(shippingInfo));
        when(shippingInfoRepository.save(any(ShippingInfo.class)))
                .thenReturn(shippingInfo);

        // When
        listener.handleOrderStatusUpdate(messageBody);

        // Then
        verify(shippingInfoRepository).findByOrderId("ORD-12345678");
        verify(shippingInfoRepository).save(any(ShippingInfo.class));
    }
} 
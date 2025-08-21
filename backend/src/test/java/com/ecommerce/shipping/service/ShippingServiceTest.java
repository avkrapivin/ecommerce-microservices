package com.ecommerce.shipping.service;

import com.ecommerce.shipping.dto.ShippingInfoDto;
import com.ecommerce.shipping.exception.ShippingInfoNotFoundException;
import com.ecommerce.shipping.model.ShippingInfo;
import com.ecommerce.shipping.model.ShippingStatus;
import com.ecommerce.shipping.repository.ShippingInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {

    @Mock
    private ShippingInfoRepository shippingInfoRepository;

    @InjectMocks
    private ShippingService shippingService;

    private ShippingInfo shippingInfo;

    @BeforeEach
    void setUp() {
        shippingInfo = new ShippingInfo();
        shippingInfo.setId(1L);
        shippingInfo.setOrderId(123L);
        shippingInfo.setShippoRateId("rate123");
        shippingInfo.setShippoTransactionId("transaction123");
        shippingInfo.setTrackingNumber("TRACK123");
        shippingInfo.setTrackingUrl("https://tracking.com/TRACK123");
        shippingInfo.setLabelUrl("https://label.com/label.pdf");
        shippingInfo.setStatus(ShippingStatus.LABEL_CREATED);
        shippingInfo.setCreatedAt(LocalDateTime.now());
        shippingInfo.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getShippingInfo_WhenExists_ShouldReturnInfo() {
        when(shippingInfoRepository.findByOrderId(123L)).thenReturn(Optional.of(shippingInfo));

        ShippingInfoDto result = shippingService.getShippingInfo(123L);

        assertNotNull(result);
        assertEquals(123L, result.getOrderId());
        assertEquals("TRACK123", result.getTrackingNumber());
        assertEquals(ShippingStatus.LABEL_CREATED, result.getStatus());
    }

    @Test
    void getShippingInfo_WhenNotFound_ShouldThrowException() {
        when(shippingInfoRepository.findByOrderId(123L)).thenReturn(Optional.empty());

        assertThrows(ShippingInfoNotFoundException.class, () -> 
            shippingService.getShippingInfo(123L)
        );
    }

    @Test
    void getShippingInfoByTracking_WhenExists_ShouldReturnInfo() {
        when(shippingInfoRepository.findByTrackingNumberOrShippoShipmentId("TRACK123", null))
                .thenReturn(Optional.of(shippingInfo));

        ShippingInfoDto result = shippingService.getShippingInfoByTracking("TRACK123");

        assertNotNull(result);
        assertEquals(123L, result.getOrderId());
        assertEquals("TRACK123", result.getTrackingNumber());
    }

    @Test
    void getShippingInfoByTracking_WhenNotFound_ShouldThrowException() {
        when(shippingInfoRepository.findByTrackingNumberOrShippoShipmentId("TRACK123", null))
                .thenReturn(Optional.empty());

        assertThrows(ShippingInfoNotFoundException.class, () -> 
            shippingService.getShippingInfoByTracking("TRACK123")
        );
    }

    @Test
    void createShippingInfo_WhenNewOrder_ShouldCreateInfo() {
        when(shippingInfoRepository.existsByOrderId(123L)).thenReturn(false);
        when(shippingInfoRepository.save(any())).thenReturn(shippingInfo);

        shippingService.createShippingInfo(123L);

        verify(shippingInfoRepository).existsByOrderId(123L);
        verify(shippingInfoRepository).save(any());
    }

    @Test
    void createShippingInfo_WhenOrderExists_ShouldNotCreateInfo() {
        when(shippingInfoRepository.existsByOrderId(123L)).thenReturn(true);

        shippingService.createShippingInfo(123L);

        verify(shippingInfoRepository).existsByOrderId(123L);
        verify(shippingInfoRepository, never()).save(any());
    }

    @Test
    void updateShippingInfo_WhenOrderExists_ShouldUpdateInfo() {
        when(shippingInfoRepository.findByOrderId(123L)).thenReturn(Optional.of(shippingInfo));
        when(shippingInfoRepository.save(any())).thenReturn(shippingInfo);

        shippingService.updateShippingInfo(123L, "ship_123", "TRACK123", 
                "https://tracking.com/TRACK123", "https://label.com/label.pdf", 
                "DHL", "Express", "15.99", "USD", "3-5");

        verify(shippingInfoRepository).findByOrderId(123L);
        verify(shippingInfoRepository).save(any());
    }

    @Test
    void updateShippingInfo_WhenOrderNotExists_ShouldCreateNewInfo() {
        when(shippingInfoRepository.findByOrderId(123L)).thenReturn(Optional.empty());
        when(shippingInfoRepository.save(any())).thenReturn(shippingInfo);

        shippingService.updateShippingInfo(123L, "ship_123", "TRACK123", 
                "https://tracking.com/TRACK123", "https://label.com/label.pdf", 
                "DHL", "Express", "15.99", "USD", "3-5");

        verify(shippingInfoRepository).findByOrderId(123L);
        verify(shippingInfoRepository).save(any());
    }
} 
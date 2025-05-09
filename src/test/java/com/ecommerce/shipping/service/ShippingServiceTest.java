package com.ecommerce.shipping.service;

import com.ecommerce.shipping.dto.*;
import com.ecommerce.shipping.exception.ShippingInfoAlreadyExistsException;
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

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {

    @Mock
    private ShippoService shippoService;

    @Mock
    private ShippingInfoRepository shippingInfoRepository;

    @InjectMocks
    private ShippingService shippingService;

    private ShippingLabelRequestDto labelRequest;
    private ShippingLabelResponseDto labelResponse;
    private ShippingInfo shippingInfo;

    @BeforeEach
    void setUp() {
        labelRequest = new ShippingLabelRequestDto();
        labelRequest.setOrderId("order123");
        labelRequest.setRateId("rate123");

        labelResponse = new ShippingLabelResponseDto();
        labelResponse.setObjectId("transaction123");
        labelResponse.setTrackingNumber("TRACK123");
        labelResponse.setTrackingUrlProvider("https://tracking.com/TRACK123");
        labelResponse.setLabelUrl("https://label.com/label.pdf");

        shippingInfo = new ShippingInfo();
        shippingInfo.setId(1L);
        shippingInfo.setOrderId("order123");
        shippingInfo.setShippoRateId("rate123");
        shippingInfo.setShippoTransactionId("transaction123");
        shippingInfo.setTrackingNumber("TRACK123");
        shippingInfo.setTrackingUrl("https://tracking.com/TRACK123");
        shippingInfo.setLabelUrl("https://label.com/label.pdf");
        shippingInfo.setStatus(ShippingStatus.LABEL_CREATED);
    }

    @Test
    void generateShippingLabel_WhenNewOrder_ShouldCreateLabel() throws IOException {
        when(shippingInfoRepository.existsByOrderId("order123")).thenReturn(false);
        when(shippoService.generateShippingLabel(any())).thenReturn(labelResponse);
        when(shippingInfoRepository.save(any())).thenReturn(shippingInfo);

        ShippingLabelResponseDto result = shippingService.generateShippingLabel(labelRequest);

        assertNotNull(result);
        assertEquals("transaction123", result.getObjectId());
        assertEquals("TRACK123", result.getTrackingNumber());
        verify(shippingInfoRepository).save(any());
    }

    @Test
    void generateShippingLabel_WhenOrderExists_ShouldThrowException() {
        when(shippingInfoRepository.existsByOrderId("order123")).thenReturn(true);

        assertThrows(ShippingInfoAlreadyExistsException.class, () -> 
            shippingService.generateShippingLabel(labelRequest)
        );
    }

    @Test
    void getShippingInfo_WhenExists_ShouldReturnInfo() {
        when(shippingInfoRepository.findByOrderId("order123")).thenReturn(Optional.of(shippingInfo));

        ShippingInfoDto result = shippingService.getShippingInfo("order123");

        assertNotNull(result);
        assertEquals("order123", result.getOrderId());
        assertEquals("TRACK123", result.getTrackingNumber());
    }

    @Test
    void getShippingInfo_WhenNotFound_ShouldThrowException() {
        when(shippingInfoRepository.findByOrderId("order123")).thenReturn(Optional.empty());

        assertThrows(ShippingInfoNotFoundException.class, () -> 
            shippingService.getShippingInfo("order123")
        );
    }

    @Test
    void updateShippingStatus_WhenExists_ShouldUpdateStatus() {
        when(shippingInfoRepository.findByOrderId("order123")).thenReturn(Optional.of(shippingInfo));
        when(shippingInfoRepository.save(any())).thenReturn(shippingInfo);

        shippingService.updateShippingStatus("order123", ShippingStatus.IN_TRANSIT);

        verify(shippingInfoRepository).save(any());
    }

    @Test
    void updateShippingStatus_WhenNotFound_ShouldThrowException() {
        when(shippingInfoRepository.findByOrderId("order123")).thenReturn(Optional.empty());

        assertThrows(ShippingInfoNotFoundException.class, () -> 
            shippingService.updateShippingStatus("order123", ShippingStatus.IN_TRANSIT)
        );
    }
} 
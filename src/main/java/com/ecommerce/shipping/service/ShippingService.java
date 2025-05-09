package com.ecommerce.shipping.service;

import com.ecommerce.shipping.dto.*;
import com.ecommerce.shipping.exception.ShippingInfoAlreadyExistsException;
import com.ecommerce.shipping.exception.ShippingInfoNotFoundException;
import com.ecommerce.shipping.model.ShippingInfo;
import com.ecommerce.shipping.model.ShippingStatus;
import com.ecommerce.shipping.repository.ShippingInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShippingService {
    private final ShippoService shippoService;
    private final ShippingInfoRepository shippingInfoRepository;

    public ShippingRateResponseDto calculateShippingRates(ShippingRateRequestDto request) throws IOException {
        return shippoService.calculateShippingRates(request);
    }

    @Transactional
    public ShippingLabelResponseDto generateShippingLabel(ShippingLabelRequestDto request) throws IOException {
        if (shippingInfoRepository.existsByOrderId(request.getOrderId())) {
            throw new ShippingInfoAlreadyExistsException("Shipping info already exists for order: " + request.getOrderId());
        }

        ShippingLabelResponseDto labelResponse = shippoService.generateShippingLabel(request);
        
        ShippingInfo shippingInfo = new ShippingInfo();
        shippingInfo.setOrderId(request.getOrderId());
        shippingInfo.setShippoRateId(request.getRateId());
        shippingInfo.setShippoTransactionId(labelResponse.getObjectId());
        shippingInfo.setTrackingNumber(labelResponse.getTrackingNumber());
        shippingInfo.setTrackingUrl(labelResponse.getTrackingUrlProvider());
        shippingInfo.setLabelUrl(labelResponse.getLabelUrl());
        shippingInfo.setStatus(ShippingStatus.LABEL_CREATED);
        
        shippingInfoRepository.save(shippingInfo);

        return labelResponse;
    }

    public ShippingInfoDto getShippingInfo(String orderId) {
        ShippingInfo shippingInfo = shippingInfoRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ShippingInfoNotFoundException("Shipping info not found for order: " + orderId));
        return convertToDto(shippingInfo);
    }

    @Transactional
    public void updateShippingStatus(String orderId, ShippingStatus status) {
        ShippingInfo shippingInfo = shippingInfoRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ShippingInfoNotFoundException("Shipping info not found for order: " + orderId));
        shippingInfo.setStatus(status);
        shippingInfoRepository.save(shippingInfo);
    }

    private ShippingInfoDto convertToDto(ShippingInfo shippingInfo) {
        ShippingInfoDto dto = new ShippingInfoDto();
        dto.setId(shippingInfo.getId());
        dto.setOrderId(shippingInfo.getOrderId());
        dto.setShippoShipmentId(shippingInfo.getShippoShipmentId());
        dto.setShippoRateId(shippingInfo.getShippoRateId());
        dto.setShippoTransactionId(shippingInfo.getShippoTransactionId());
        dto.setTrackingNumber(shippingInfo.getTrackingNumber());
        dto.setTrackingUrl(shippingInfo.getTrackingUrl());
        dto.setLabelUrl(shippingInfo.getLabelUrl());
        dto.setCarrier(shippingInfo.getCarrier());
        dto.setService(shippingInfo.getService());
        dto.setAmount(shippingInfo.getAmount());
        dto.setCurrency(shippingInfo.getCurrency());
        dto.setEstimatedDays(shippingInfo.getEstimatedDays());
        dto.setStatus(shippingInfo.getStatus());
        dto.setCreatedAt(shippingInfo.getCreatedAt());
        dto.setUpdatedAt(shippingInfo.getUpdatedAt());
        return dto;
    }
} 
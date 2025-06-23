package com.ecommerce.shipping.service;

import com.ecommerce.shipping.dto.ShippingInfoDto;
import com.ecommerce.shipping.exception.ShippingInfoNotFoundException;
import com.ecommerce.shipping.model.ShippingInfo;
import com.ecommerce.shipping.model.ShippingStatus;
import com.ecommerce.shipping.repository.ShippingInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShippingService {
    private final ShippingInfoRepository shippingInfoRepository;

    public ShippingInfoDto getShippingInfo(String orderId) {
        ShippingInfo shippingInfo = shippingInfoRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ShippingInfoNotFoundException("Shipping info not found for order: " + orderId));
        return convertToDto(shippingInfo);
    }

    public ShippingInfoDto getShippingInfoByTracking(String trackingNumber) {
        ShippingInfo shippingInfo = shippingInfoRepository.findByTrackingNumberOrShippoShipmentId(trackingNumber, null)
                .orElseThrow(() -> new ShippingInfoNotFoundException("Shipping info not found for tracking: " + trackingNumber));
        return convertToDto(shippingInfo);
    }

    @Transactional
    public void createShippingInfo(String orderId) {
        if (shippingInfoRepository.existsByOrderId(orderId)) {
            log.warn("Shipping info already exists for order: {}", orderId);
            return;
        }

        ShippingInfo shippingInfo = new ShippingInfo();
        shippingInfo.setOrderId(orderId);
        shippingInfo.setStatus(ShippingStatus.PENDING);
        
        shippingInfoRepository.save(shippingInfo);
        log.info("Created shipping info for order: {}", orderId);
    }

    @Transactional
    public void updateShippingInfo(String orderId, String shippoShipmentId, String trackingNumber, 
                                  String trackingUrl, String labelUrl, String carrier, String service, 
                                  String amount, String currency, String estimatedDays) {
        ShippingInfo shippingInfo = shippingInfoRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    ShippingInfo newInfo = new ShippingInfo();
                    newInfo.setOrderId(orderId);
                    return newInfo;
                });

        shippingInfo.setShippoShipmentId(shippoShipmentId);
        shippingInfo.setTrackingNumber(trackingNumber);
        shippingInfo.setTrackingUrl(trackingUrl);
        shippingInfo.setLabelUrl(labelUrl);
        shippingInfo.setCarrier(carrier);
        shippingInfo.setService(service);
        shippingInfo.setAmount(amount);
        shippingInfo.setCurrency(currency);
        shippingInfo.setEstimatedDays(estimatedDays);
        shippingInfo.setStatus(ShippingStatus.LABEL_CREATED);

        shippingInfoRepository.save(shippingInfo);
        log.info("Updated shipping info for order: {}", orderId);
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
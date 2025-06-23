package com.ecommerce.shipping.event;

import com.ecommerce.shipping.model.ShippingInfo;
import com.ecommerce.shipping.model.ShippingStatus;
import com.ecommerce.shipping.repository.ShippingInfoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusUpdateListener {

    private final ShippingInfoRepository shippingInfoRepository;
    private final ObjectMapper objectMapper;

    public void handleOrderStatusUpdate(String messageBody) {
        try {
            log.info("Received order status update: {}", messageBody);

            JsonNode jsonNode = objectMapper.readTree(messageBody);
            String orderId = jsonNode.get("orderId").asText();
            String status = jsonNode.get("status").asText();
            String trackingNumber = jsonNode.has("trackingNumber") ? 
                jsonNode.get("trackingNumber").asText() : null;

            // Находим запись о доставке
            Optional<ShippingInfo> shippingInfoOpt = shippingInfoRepository.findByOrderId(orderId);
            
            if (shippingInfoOpt.isPresent()) {
                ShippingInfo shippingInfo = shippingInfoOpt.get();
                
                // Обновляем статус
                ShippingStatus shippingStatus = mapToShippingStatus(status);
                shippingInfo.setStatus(shippingStatus);
                
                // Обновляем tracking number если он есть
                if (trackingNumber != null && !trackingNumber.isEmpty()) {
                    shippingInfo.setTrackingNumber(trackingNumber);
                }
                
                shippingInfoRepository.save(shippingInfo);
                log.info("Updated shipping status for order {}: {}", orderId, status);
            } else {
                log.warn("Shipping info not found for order: {}", orderId);
            }
        } catch (Exception e) {
            log.error("Error processing order status update: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process order status update", e);
        }
    }

    private ShippingStatus mapToShippingStatus(String status) {
        return switch (status.toUpperCase()) {
            case "PENDING" -> ShippingStatus.PENDING;
            case "PROCESSING" -> ShippingStatus.PROCESSING;
            case "LABEL_CREATED" -> ShippingStatus.LABEL_CREATED;
            case "SHIPPED" -> ShippingStatus.SHIPPED;
            case "IN_TRANSIT" -> ShippingStatus.IN_TRANSIT;
            case "DELIVERED" -> ShippingStatus.DELIVERED;
            case "FAILED" -> ShippingStatus.FAILED;
            case "CANCELLED" -> ShippingStatus.CANCELLED;
            default -> {
                log.warn("Unknown shipping status: {}, defaulting to PENDING", status);
                yield ShippingStatus.PENDING;
            }
        };
    }
} 
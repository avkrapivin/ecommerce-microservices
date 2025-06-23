package com.ecommerce.shipping.controller;

import com.ecommerce.shipping.model.ShippingInfo;
import com.ecommerce.shipping.model.ShippingStatus;
import com.ecommerce.shipping.repository.ShippingInfoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/delivery")
@RequiredArgsConstructor
@Slf4j
public class ShippoWebhookController {

    private final ShippingInfoRepository shippingInfoRepository;
    private final ObjectMapper objectMapper;

    @PostMapping("/shippo-webhook")
    public ResponseEntity<String> handleShippoWebhook(@RequestBody String payload) {
        try {
            log.info("Received Shippo webhook: {}", payload);
            
            JsonNode jsonNode = objectMapper.readTree(payload);
            
            // Извлекаем данные из webhook'а Shippo
            String trackingNumber = jsonNode.path("tracking_number").asText();
            String status = jsonNode.path("tracking_status").path("status").asText();
            String shipmentId = jsonNode.path("shipment_id").asText();
            
            log.info("Shippo webhook - tracking: {}, status: {}, shipment: {}", 
                    trackingNumber, status, shipmentId);
            
            // Находим запись о доставке по tracking number или shipment ID
            Optional<ShippingInfo> shippingInfoOpt = shippingInfoRepository
                    .findByTrackingNumberOrShippoShipmentId(trackingNumber, shipmentId);
            
            if (shippingInfoOpt.isPresent()) {
                ShippingInfo shippingInfo = shippingInfoOpt.get();
                ShippingStatus shippingStatus = mapShippoStatusToShippingStatus(status);
                
                shippingInfo.setStatus(shippingStatus);
                shippingInfoRepository.save(shippingInfo);
                
                log.info("Updated shipping status for tracking {}: {}", trackingNumber, status);
                return ResponseEntity.ok("Webhook processed successfully");
            } else {
                log.warn("Shipping info not found for tracking: {} or shipment: {}", 
                        trackingNumber, shipmentId);
                return ResponseEntity.ok("Webhook received but no matching shipping info found");
            }
            
        } catch (Exception e) {
            log.error("Error processing Shippo webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }
    
    private ShippingStatus mapShippoStatusToShippingStatus(String shippoStatus) {
        return switch (shippoStatus.toUpperCase()) {
            case "UNKNOWN" -> ShippingStatus.PENDING;
            case "DELIVERED" -> ShippingStatus.DELIVERED;
            case "TRANSIT" -> ShippingStatus.IN_TRANSIT;
            case "FAILURE" -> ShippingStatus.FAILED;
            case "RETURNED" -> ShippingStatus.CANCELLED;
            default -> {
                log.warn("Unknown Shippo status: {}, defaulting to PENDING", shippoStatus);
                yield ShippingStatus.PENDING;
            }
        };
    }
} 
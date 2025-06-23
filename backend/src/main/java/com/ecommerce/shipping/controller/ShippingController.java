package com.ecommerce.shipping.controller;

import com.ecommerce.shipping.dto.ShippingInfoDto;
import com.ecommerce.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
public class ShippingController {
    private final ShippingService shippingService;

    @GetMapping("/{orderId}")
    public ResponseEntity<ShippingInfoDto> getShippingInfo(@PathVariable String orderId) {
        ShippingInfoDto shippingInfo = shippingService.getShippingInfo(orderId);
        return ResponseEntity.ok(shippingInfo);
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ShippingInfoDto> getShippingInfoByTracking(@PathVariable String trackingNumber) {
        ShippingInfoDto shippingInfo = shippingService.getShippingInfoByTracking(trackingNumber);
        return ResponseEntity.ok(shippingInfo);
    }
} 
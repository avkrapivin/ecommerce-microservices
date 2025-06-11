package com.ecommerce.shipping.controller;

import com.ecommerce.shipping.dto.*;
import com.ecommerce.shipping.model.ShippingInfo;
import com.ecommerce.shipping.model.ShippingStatus;
import com.ecommerce.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
public class ShippingController {
    private final ShippingService shippingService;

    @PostMapping("/rates")
    public ResponseEntity<ShippingRateResponseDto> calculateShippingRates(@RequestBody ShippingRateRequestDto request) throws IOException {
        ShippingRateResponseDto response = shippingService.calculateShippingRates(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/labels")
    public ResponseEntity<ShippingLabelResponseDto> generateShippingLabel(@RequestBody ShippingLabelRequestDto request) throws IOException {
        ShippingLabelResponseDto response = shippingService.generateShippingLabel(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/labels/{orderId}")
    public ResponseEntity<ShippingInfoDto> getShippingLabel(@PathVariable String orderId) {
        ShippingInfoDto shippingInfo = shippingService.getShippingInfo(orderId);
        return ResponseEntity.ok(shippingInfo);
    }

    @PatchMapping("/status/{orderId}")
    public ResponseEntity<Void> updateShippingStatus(
            @PathVariable String orderId,
            @RequestParam ShippingStatus status) {
        shippingService.updateShippingStatus(orderId, status);
        return ResponseEntity.ok().build();
    }
} 
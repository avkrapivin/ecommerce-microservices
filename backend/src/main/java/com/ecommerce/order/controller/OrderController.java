package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.UpdateOrderStatusDto;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderDto> getOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderDto>> getUserOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<OrderDto> createOrder(
            @PathVariable Long userId,
            @RequestBody @Valid OrderRequest request) {
        OrderDto createdOrder = orderService.createOrder(userId, request);
        return ResponseEntity.created(URI.create("/orders/" + createdOrder.getId()))
        .body(createdOrder);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody @Valid UpdateOrderStatusDto request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }

    // Admin endpoints
    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderDto>> getAllOrders(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getAllOrders(page, size));
    }

    @GetMapping("/admin/orders/recent")  
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderDto>> getRecentOrders(
            @RequestParam(required = false, defaultValue = "10") int limit) {
        return ResponseEntity.ok(orderService.getRecentOrders(limit));
    }

    @PutMapping("/admin/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDto> adminUpdateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody @Valid UpdateOrderStatusDto request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request));
    }

} 
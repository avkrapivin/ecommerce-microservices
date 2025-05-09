package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderDto;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.UpdateOrderStatusDto;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.payment.dto.PayPalPaymentResponse;
import com.paypal.base.rest.PayPalRESTException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/{orderId}/payment")
    public ResponseEntity<PayPalPaymentResponse> createPayment(@PathVariable Long orderId) {
        try {
            PayPalPaymentResponse response = orderService.createPayment(orderId);
            return ResponseEntity.ok(response);
        } catch (PayPalRESTException e) {
            return ResponseEntity.badRequest().body(PayPalPaymentResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/payment/success")
    public ResponseEntity<Void> handlePaymentSuccess(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId) {
        try {
            orderService.processSuccessfulPayment(paymentId, payerId);
            return ResponseEntity.ok().build();
        } catch (PayPalRESTException e) {
            orderService.handlePaymentFailure(paymentId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/payment/cancel")
    public ResponseEntity<Void> handlePaymentCancellation(@RequestParam("paymentId") String paymentId) {
        orderService.handlePaymentCancellation(paymentId);
        return ResponseEntity.ok().build();
    }
} 
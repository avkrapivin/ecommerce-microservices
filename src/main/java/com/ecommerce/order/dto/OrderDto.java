package com.ecommerce.order.dto;

import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentStatus;
import com.ecommerce.user.dto.UserDto;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDto {
    private Long id;
    private String orderNumber;
    private UserDto user;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private ShippingAddressDto shippingAddress;
    private List<OrderItemDto> items;
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal tax;
    private BigDecimal total;
    private String paymentId;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 
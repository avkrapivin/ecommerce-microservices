package com.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderDto {
    @NotNull(message = "Shipping address is required")
    @Valid
    private ShippingAddressDto shippingAddress;
    
    @NotEmpty(message = "Order items are required")
    @Valid
    private List<OrderItemDto> items;
} 
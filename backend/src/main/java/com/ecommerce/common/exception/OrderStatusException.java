package com.ecommerce.common.exception;

public class OrderStatusException extends IllegalStateException {
    public OrderStatusException(String message) {
        super(message);
    }
} 
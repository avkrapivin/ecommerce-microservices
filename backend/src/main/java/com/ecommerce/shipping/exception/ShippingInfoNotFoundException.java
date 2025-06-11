package com.ecommerce.shipping.exception;

public class ShippingInfoNotFoundException extends RuntimeException {
    public ShippingInfoNotFoundException(String message) {
        super(message);
    }
} 
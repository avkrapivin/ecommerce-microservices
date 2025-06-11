package com.ecommerce.shipping.exception;

public class ShippingInfoAlreadyExistsException extends RuntimeException {
    public ShippingInfoAlreadyExistsException(String message) {
        super(message);
    }
} 
package com.ecommerce.lambda.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class Order {
    private String id;
    private String orderNumber;
    private BigDecimal total;
    private List<OrderItem> items;
    private String userId;
    private String status;
    private String paymentStatus;
    private Address shippingAddress;
    private Address billingAddress;

    @Data
    public static class OrderItem {
        private String productId;
        private String name;
        private BigDecimal price;
        private Integer quantity;
    }

    @Data
    public static class Address {
        private String street;
        private String city;
        private String state;
        private String country;
        private String zipCode;
    }
} 
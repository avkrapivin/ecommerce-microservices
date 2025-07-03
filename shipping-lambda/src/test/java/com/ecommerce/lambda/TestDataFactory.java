package com.ecommerce.lambda;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.ecommerce.lambda.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

public class TestDataFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Order createTestOrder() {
        return createTestOrder("order-123", "ORD-123", "customer-123", new BigDecimal("100.00"));
    }

    public static Order createTestOrder(String id, String orderNumber, String userId, BigDecimal total) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber(orderNumber);
        order.setUserId(userId);
        order.setTotal(total);
        order.setStatus("PENDING");
        order.setPaymentStatus("PENDING");
        order.setShippingAddress(createTestAddress());
        order.setBillingAddress(createTestAddress());
        order.setItems(List.of(createTestOrderItem()));
        return order;
    }

    public static Order.Address createTestAddress() {
        Order.Address address = new Order.Address();
        address.setStreet("123 Main St");
        address.setCity("New York");
        address.setState("NY");
        address.setCountry("US");
        address.setZipCode("10001");
        return address;
    }

    public static Order.OrderItem createTestOrderItem() {
        Order.OrderItem item = new Order.OrderItem();
        item.setProductId("prod-123");
        item.setName("Test Product");
        item.setPrice(new BigDecimal("50.00"));
        item.setQuantity(2);
        return item;
    }

    public static SNSEvent createSnsEvent(String message) {
        SNSEvent event = new SNSEvent();
        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessage(message);
        record.setSns(sns);
        event.setRecords(List.of(record));
        return event;
    }

    public static SNSEvent createSnsEventWithMultipleRecords(String... messages) {
        SNSEvent event = new SNSEvent();
        List<SNSEvent.SNSRecord> records = new java.util.ArrayList<>();
        
        for (String message : messages) {
            SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
            SNSEvent.SNS sns = new SNSEvent.SNS();
            sns.setMessage(message);
            record.setSns(sns);
            records.add(record);
        }
        
        event.setRecords(records);
        return event;
    }

    public static String orderToJson(Order order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize order", e);
        }
    }

    public static Order createLargeOrder() {
        return createTestOrder("order-large", "ORD-LARGE", "customer-large", new BigDecimal("9999.99"));
    }

    public static Order createOrderWithSpecialCharacters() {
        Order order = createTestOrder();
        order.setOrderNumber("ORD-123-特殊字符-@#$%");
        return order;
    }

    public static Order createOrderWithZeroAmount() {
        return createTestOrder("order-zero", "ORD-ZERO", "customer-zero", BigDecimal.ZERO);
    }

    public static Order createOrderWithoutAddress() {
        Order order = createTestOrder();
        order.setShippingAddress(null);
        order.setBillingAddress(null);
        return order;
    }
} 
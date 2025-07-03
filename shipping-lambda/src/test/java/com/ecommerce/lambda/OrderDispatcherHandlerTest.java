package com.ecommerce.lambda;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.ecommerce.lambda.model.Order;
import com.ecommerce.lambda.service.OrderDispatcherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDispatcherHandlerTest {

    @Mock
    private OrderDispatcherService orderDispatcherService;

    private OrderDispatcherHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Используем конструктор для тестирования с моками
        handler = new OrderDispatcherHandler(orderDispatcherService, objectMapper);
    }

    @Test
    void shouldHandleSnsEventSuccessfully() throws Exception {
        // Given
        Order order = TestDataFactory.createTestOrder();
        String orderJson = TestDataFactory.orderToJson(order);
        
        SNSEvent event = TestDataFactory.createSnsEvent(orderJson);

        // When
        handler.handleRequest(event, null);

        // Then
        // Verify that the handler processes the event without throwing exceptions
        verify(orderDispatcherService).processOrder(order);
    }

    @Test
    void shouldHandleMultipleSnsRecords() throws Exception {
        // Given
        Order order1 = TestDataFactory.createTestOrder();
        Order order2 = TestDataFactory.createTestOrder();
        order2.setId("order-456");
        order2.setOrderNumber("ORD-456");
        
        String order1Json = TestDataFactory.orderToJson(order1);
        String order2Json = TestDataFactory.orderToJson(order2);
        
        SNSEvent event = TestDataFactory.createSnsEventWithMultipleRecords(order1Json, order2Json);

        // When
        handler.handleRequest(event, null);

        // Then
        // Verify that the handler processes multiple records
        verify(orderDispatcherService).processOrder(order1);
        verify(orderDispatcherService).processOrder(order2);
    }

    @Test
    void shouldThrowExceptionWhenInvalidJson() {
        // Given
        String invalidJson = "{ invalid json }";
        SNSEvent event = TestDataFactory.createSnsEvent(invalidJson);

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(event, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order");
    }

    @Test
    void shouldHandleEmptyEvent() {
        // Given
        SNSEvent event = new SNSEvent();
        event.setRecords(java.util.List.of());

        // When
        handler.handleRequest(event, null);

        // Then
        // Should handle empty event gracefully
        verifyNoInteractions(orderDispatcherService);
    }

    @Test
    void shouldHandleNullEvent() {
        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order");
    }

    @Test
    void shouldHandleLargeOrder() throws Exception {
        // Given
        Order order = TestDataFactory.createLargeOrder();
        String orderJson = TestDataFactory.orderToJson(order);
        
        SNSEvent event = TestDataFactory.createSnsEvent(orderJson);

        // When
        handler.handleRequest(event, null);

        // Then
        verify(orderDispatcherService).processOrder(order);
    }

    @Test
    void shouldHandleOrderWithSpecialCharacters() throws Exception {
        // Given
        Order order = TestDataFactory.createOrderWithSpecialCharacters();
        String orderJson = TestDataFactory.orderToJson(order);
        
        SNSEvent event = TestDataFactory.createSnsEvent(orderJson);

        // When
        handler.handleRequest(event, null);

        // Then
        verify(orderDispatcherService).processOrder(order);
    }

    @Test
    void shouldHandleOrderWithZeroAmount() throws Exception {
        // Given
        Order order = TestDataFactory.createOrderWithZeroAmount();
        String orderJson = TestDataFactory.orderToJson(order);
        
        SNSEvent event = TestDataFactory.createSnsEvent(orderJson);

        // When
        handler.handleRequest(event, null);

        // Then
        verify(orderDispatcherService).processOrder(order);
    }

    @Test
    void shouldHandleOrderWithoutAddress() throws Exception {
        // Given
        Order order = TestDataFactory.createOrderWithoutAddress();
        String orderJson = TestDataFactory.orderToJson(order);
        
        SNSEvent event = TestDataFactory.createSnsEvent(orderJson);

        // When
        handler.handleRequest(event, null);

        // Then
        verify(orderDispatcherService).processOrder(order);
    }

    @Test
    void shouldHandleMalformedSnsRecord() {
        // Given
        SNSEvent event = new SNSEvent();
        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        // SNS record without SNS object
        event.setRecords(java.util.List.of(record));

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(event, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order");
    }

    @Test
    void shouldHandleNullSnsMessage() {
        // Given
        SNSEvent event = new SNSEvent();
        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessage(null);
        record.setSns(sns);
        event.setRecords(java.util.List.of(record));

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(event, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order");
    }

    @Test
    void shouldHandleOrderDispatcherServiceException() throws Exception {
        // Given
        Order order = TestDataFactory.createTestOrder();
        String orderJson = TestDataFactory.orderToJson(order);
        
        SNSEvent event = TestDataFactory.createSnsEvent(orderJson);
        
        doThrow(new RuntimeException("Service error"))
            .when(orderDispatcherService).processOrder(any(Order.class));

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(event, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order");
    }
} 
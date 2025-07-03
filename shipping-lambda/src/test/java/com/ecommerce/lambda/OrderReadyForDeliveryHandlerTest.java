package com.ecommerce.lambda;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.ecommerce.lambda.model.PaymentCompletedEvent;
import com.ecommerce.lambda.service.OrderDeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderReadyForDeliveryHandlerTest {

    @Mock
    private OrderDeliveryService orderDeliveryService;

    private OrderReadyForDeliveryHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Используем конструктор для тестирования с моками
        handler = new OrderReadyForDeliveryHandler(orderDeliveryService, objectMapper);
    }

    @Test
    void shouldHandleSnsEventSuccessfully() throws Exception {
        // Given
        PaymentCompletedEvent event = createTestPaymentEvent();
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verify(orderDeliveryService).prepareOrderForDelivery(eq(event));
    }

    @Test
    void shouldHandleMultipleSnsRecords() throws Exception {
        // Given
        PaymentCompletedEvent event1 = createTestPaymentEvent();
        PaymentCompletedEvent event2 = createTestPaymentEvent();
        event2.setOrderId("order-456");
        event2.setOrderNumber("ORD-456");
        
        String event1Json = objectMapper.writeValueAsString(event1);
        String event2Json = objectMapper.writeValueAsString(event2);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEventWithMultipleRecords(event1Json, event2Json);

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verify(orderDeliveryService, times(2)).prepareOrderForDelivery(any(PaymentCompletedEvent.class));
    }

    @Test
    void shouldThrowExceptionWhenInvalidJson() {
        // Given
        String invalidJson = "{ invalid json }";
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(invalidJson);

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(snsEvent, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process SNS event");
    }

    @Test
    void shouldHandleEmptyEvent() {
        // Given
        SNSEvent snsEvent = new SNSEvent();
        snsEvent.setRecords(java.util.List.of());

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verifyNoInteractions(orderDeliveryService);
    }

    @Test
    void shouldHandleNullEvent() {
        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process SNS event");
    }

    @Test
    void shouldHandleEventWithLargeParcel() throws Exception {
        // Given
        PaymentCompletedEvent event = createLargeParcelEvent();
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verify(orderDeliveryService).prepareOrderForDelivery(eq(event));
    }

    @Test
    void shouldHandleEventWithSpecialCharacters() throws Exception {
        // Given
        PaymentCompletedEvent event = createTestPaymentEvent();
        event.setOrderNumber("ORD-123-特殊字符-@#$%");
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verify(orderDeliveryService).prepareOrderForDelivery(eq(event));
    }

    @Test
    void shouldHandleEventWithNullFields() throws Exception {
        // Given
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        // Other fields are null
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verify(orderDeliveryService).prepareOrderForDelivery(eq(event));
    }

    @Test
    void shouldHandleMalformedSnsRecord() {
        // Given
        SNSEvent snsEvent = new SNSEvent();
        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        // SNS record without SNS object
        snsEvent.setRecords(java.util.List.of(record));

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(snsEvent, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process SNS event");
    }

    @Test
    void shouldHandleNullSnsMessage() {
        // Given
        SNSEvent snsEvent = new SNSEvent();
        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessage(null);
        record.setSns(sns);
        snsEvent.setRecords(java.util.List.of(record));

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(snsEvent, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process SNS event");
    }

    @Test
    void shouldHandleOrderDeliveryServiceException() throws Exception {
        // Given
        PaymentCompletedEvent event = createTestPaymentEvent();
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);
        
        doThrow(new RuntimeException("Service error"))
            .when(orderDeliveryService).prepareOrderForDelivery(any(PaymentCompletedEvent.class));

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(snsEvent, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process SNS event");
    }

    private PaymentCompletedEvent createTestPaymentEvent() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        event.setCustomerName("John Doe");
        event.setShippingAddress("123 Main St");
        event.setShippingCity("New York");
        event.setShippingState("NY");
        event.setShippingZip("10001");
        event.setShippingCountry("US");
        event.setPhoneNumber("+1234567890");
        event.setParcelLength(10.0);
        event.setParcelWidth(5.0);
        event.setParcelHeight(2.0);
        event.setParcelDistanceUnit("in");
        event.setParcelWeight(1.5);
        event.setParcelMassUnit("lb");
        return event;
    }

    private PaymentCompletedEvent createLargeParcelEvent() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId("order-large");
        event.setOrderNumber("ORD-LARGE");
        event.setCustomerName("John Doe");
        event.setShippingAddress("123 Main St");
        event.setShippingCity("New York");
        event.setShippingState("NY");
        event.setShippingZip("10001");
        event.setShippingCountry("US");
        event.setPhoneNumber("+1234567890");
        event.setParcelLength(50.0);
        event.setParcelWidth(30.0);
        event.setParcelHeight(20.0);
        event.setParcelDistanceUnit("in");
        event.setParcelWeight(25.0);
        event.setParcelMassUnit("lb");
        return event;
    }
} 
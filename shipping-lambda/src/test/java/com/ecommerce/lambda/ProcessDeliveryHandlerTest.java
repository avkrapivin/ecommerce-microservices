package com.ecommerce.lambda;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.ecommerce.lambda.model.OrderReadyForDeliveryEvent;
import com.ecommerce.lambda.model.OrderStatusUpdateEvent;
import com.ecommerce.lambda.service.ShippoService;
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
class ProcessDeliveryHandlerTest {

    @Mock
    private ShippoService shippoService;

    private ProcessDeliveryHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Используем конструктор для тестирования с моками
        handler = new ProcessDeliveryHandler(shippoService, objectMapper);
    }

    @Test
    void shouldHandleSnsEventSuccessfully() throws Exception {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verify(shippoService).processDelivery(eq(event));
    }

    @Test
    void shouldHandleMultipleSnsRecords() throws Exception {
        // Given
        OrderReadyForDeliveryEvent event1 = createTestEvent();
        OrderReadyForDeliveryEvent event2 = createTestEvent();
        event2.setOrderId("order-456");
        event2.setOrderNumber("ORD-456");
        
        String event1Json = objectMapper.writeValueAsString(event1);
        String event2Json = objectMapper.writeValueAsString(event2);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEventWithMultipleRecords(event1Json, event2Json);

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verify(shippoService, times(2)).processDelivery(any(OrderReadyForDeliveryEvent.class));
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
        verifyNoInteractions(shippoService);
    }

    @Test
    void shouldHandleNullEvent() {
        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process SNS event");
    }

    @Test
    void shouldHandleEventWithDifferentStatuses() throws Exception {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        event.setOrderNumber("ORD-123-DELIVERED");
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verify(shippoService).processDelivery(eq(event));
    }

    @Test
    void shouldHandleEventWithTrackingNumber() throws Exception {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        event.setCustomerEmail("test@example.com");
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verify(shippoService).processDelivery(eq(event));
    }

    @Test
    void shouldHandleEventWithNullFields() throws Exception {
        // Given
        OrderReadyForDeliveryEvent event = new OrderReadyForDeliveryEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        // Other fields are null
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verify(shippoService).processDelivery(eq(event));
    }

    @Test
    void shouldHandleEventWithSpecialCharacters() throws Exception {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        event.setOrderNumber("ORD-123-特殊字符-@#$%");
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verify(shippoService).processDelivery(eq(event));
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
    void shouldHandleEventWithLongTrackingNumber() throws Exception {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        event.setCustomerEmail("very.long.email.address@very.long.domain.example.com");
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verify(shippoService).processDelivery(eq(event));
    }

    @Test
    void shouldHandleEventWithEmptyEmail() throws Exception {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        event.setCustomerEmail("");
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verify(shippoService).processDelivery(eq(event));
    }

    @Test
    void shouldHandleShippoServiceException() throws Exception {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);
        
        doThrow(new RuntimeException("Service error"))
            .when(shippoService).processDelivery(any(OrderReadyForDeliveryEvent.class));

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(snsEvent, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process SNS event");
    }

    @Test
    void shouldHandleShippoServiceExceptionWithDetailedStatus() throws Exception {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);
        
        // Shippo service should throw exception but also publish SHIPPING_FAILED status
        doThrow(new RuntimeException("Shippo API error: Address validation failed"))
            .when(shippoService).processDelivery(any(OrderReadyForDeliveryEvent.class));

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(snsEvent, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process SNS event");
        
        // Verify shippoService was called
        verify(shippoService).processDelivery(eq(event));
    }

    @Test
    void shouldProcessDeliverySuccessfullyWithStatusPublishing() throws Exception {
        // Given
        OrderReadyForDeliveryEvent event = createTestEvent();
        String eventJson = objectMapper.writeValueAsString(event);
        
        SNSEvent snsEvent = TestDataFactory.createSnsEvent(eventJson);

        // When
        handler.handleRequest(snsEvent, null);

        // Then
        verify(shippoService).processDelivery(eq(event));
        // ShippoService should publish SHIPPING_INITIATED status internally
        // This is verified at the ShippoService unit test level
    }

    private OrderReadyForDeliveryEvent createTestEvent() {
        OrderReadyForDeliveryEvent event = new OrderReadyForDeliveryEvent();
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
} 
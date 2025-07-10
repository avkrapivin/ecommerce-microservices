package com.ecommerce.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.ecommerce.lambda.model.OrderReadyForDeliveryEvent;
import com.ecommerce.lambda.model.OrderStatusUpdateEvent;
import com.ecommerce.lambda.model.PaymentCompletedEvent;
import com.ecommerce.lambda.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationHandlerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private Context context;

    private NotificationHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new NotificationHandler(notificationService, objectMapper);
    }

    @Test
    void shouldProcessPaymentCompletedEvent() throws Exception {
        // Given
        PaymentCompletedEvent event = createPaymentCompletedEvent();
        String eventJson = objectMapper.writeValueAsString(event);
        SNSEvent snsEvent = createSnsEvent("arn:aws:sns:us-east-1:123456789:PaymentCompleted", eventJson);

        // When
        handler.handleRequest(snsEvent, context);

        // Then
        verify(notificationService).processPaymentCompleted(any(PaymentCompletedEvent.class));
    }

    @Test
    void shouldProcessOrderReadyForDeliveryEvent() throws Exception {
        // Given
        OrderReadyForDeliveryEvent event = createOrderReadyForDeliveryEvent();
        String eventJson = objectMapper.writeValueAsString(event);
        SNSEvent snsEvent = createSnsEvent("arn:aws:sns:us-east-1:123456789:order-ready-for-delivery", eventJson);

        // When
        handler.handleRequest(snsEvent, context);

        // Then
        verify(notificationService).processOrderReadyForDelivery(any(OrderReadyForDeliveryEvent.class));
    }

    @Test
    void shouldProcessOrderStatusUpdatedEvent() throws Exception {
        // Given
        OrderStatusUpdateEvent event = createOrderStatusUpdateEvent();
        String eventJson = objectMapper.writeValueAsString(event);
        SNSEvent snsEvent = createSnsEvent("arn:aws:sns:us-east-1:123456789:OrderStatusUpdated", eventJson);

        // When
        handler.handleRequest(snsEvent, context);

        // Then
        verify(notificationService).processOrderStatusUpdated(any(OrderStatusUpdateEvent.class));
    }

    @Test
    void shouldProcessMultipleRecords() throws Exception {
        // Given
        PaymentCompletedEvent event1 = createPaymentCompletedEvent();
        OrderStatusUpdateEvent event2 = createOrderStatusUpdateEvent();
        
        String event1Json = objectMapper.writeValueAsString(event1);
        String event2Json = objectMapper.writeValueAsString(event2);
        
        SNSEvent snsEvent = new SNSEvent();
        SNSEvent.SNSRecord record1 = createSnsRecord("arn:aws:sns:us-east-1:123456789:PaymentCompleted", event1Json);
        SNSEvent.SNSRecord record2 = createSnsRecord("arn:aws:sns:us-east-1:123456789:OrderStatusUpdated", event2Json);
        snsEvent.setRecords(Arrays.asList(record1, record2));

        // When
        handler.handleRequest(snsEvent, context);

        // Then
        verify(notificationService).processPaymentCompleted(any(PaymentCompletedEvent.class));
        verify(notificationService).processOrderStatusUpdated(any(OrderStatusUpdateEvent.class));
    }

    @Test
    void shouldSkipUnknownTopicArn() throws Exception {
        // Given
        PaymentCompletedEvent event = createPaymentCompletedEvent();
        String eventJson = objectMapper.writeValueAsString(event);
        SNSEvent snsEvent = createSnsEvent("arn:aws:sns:us-east-1:123456789:UnknownTopic", eventJson);

        // When
        handler.handleRequest(snsEvent, context);

        // Then
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldHandleInvalidJson() {
        // Given
        String invalidJson = "{ invalid json }";
        SNSEvent snsEvent = createSnsEvent("arn:aws:sns:us-east-1:123456789:PaymentCompleted", invalidJson);

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(snsEvent, context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process SNS notification event");
    }

    @Test
    void shouldHandleNotificationServiceException() throws Exception {
        // Given
        PaymentCompletedEvent event = createPaymentCompletedEvent();
        String eventJson = objectMapper.writeValueAsString(event);
        SNSEvent snsEvent = createSnsEvent("arn:aws:sns:us-east-1:123456789:PaymentCompleted", eventJson);
        
        doThrow(new RuntimeException("Service error"))
            .when(notificationService).processPaymentCompleted(any(PaymentCompletedEvent.class));

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(snsEvent, context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process SNS notification event");
    }

    @Test
    void shouldHandleEmptyRecords() {
        // Given
        SNSEvent snsEvent = new SNSEvent();
        snsEvent.setRecords(Arrays.asList());

        // When
        handler.handleRequest(snsEvent, context);

        // Then
        verifyNoInteractions(notificationService);
    }

    @Test
    void shouldHandleNullEvent() {
        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(null, context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process SNS notification event");
    }

    @Test
    void shouldHandleNullSnsMessage() {
        // Given
        SNSEvent snsEvent = new SNSEvent();
        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setTopicArn("arn:aws:sns:us-east-1:123456789:PaymentCompleted");
        sns.setMessage(null); // Null message
        record.setSns(sns);
        snsEvent.setRecords(Arrays.asList(record));

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(snsEvent, context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process SNS notification event");
    }

    private SNSEvent createSnsEvent(String topicArn, String message) {
        SNSEvent snsEvent = new SNSEvent();
        SNSEvent.SNSRecord record = createSnsRecord(topicArn, message);
        snsEvent.setRecords(Arrays.asList(record));
        return snsEvent;
    }

    private SNSEvent.SNSRecord createSnsRecord(String topicArn, String message) {
        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setTopicArn(topicArn);
        sns.setMessage(message);
        sns.setMessageId("message-123");
        record.setSns(sns);
        return record;
    }

    private PaymentCompletedEvent createPaymentCompletedEvent() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        event.setCustomerEmail("customer@example.com");
        event.setCustomerName("John Doe");
        return event;
    }

    private OrderReadyForDeliveryEvent createOrderReadyForDeliveryEvent() {
        OrderReadyForDeliveryEvent event = new OrderReadyForDeliveryEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        event.setCustomerEmail("customer@example.com");
        event.setCustomerName("John Doe");
        return event;
    }

    private OrderStatusUpdateEvent createOrderStatusUpdateEvent() {
        OrderStatusUpdateEvent event = new OrderStatusUpdateEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        event.setCustomerEmail("customer@example.com");
        event.setCustomerName("John Doe");
        event.setStatus("DELIVERED");
        return event;
    }
} 
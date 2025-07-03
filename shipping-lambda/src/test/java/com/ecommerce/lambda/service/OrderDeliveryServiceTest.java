package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.PaymentCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDeliveryServiceTest {

    @Mock
    private SnsPublisher snsPublisher;

    private OrderDeliveryService orderDeliveryService;
    private ArgumentCaptor<String> topicArnCaptor;
    private ArgumentCaptor<String> messageCaptor;

    private static final String ORDER_READY_FOR_DELIVERY_TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:order-ready-for-delivery";

    @BeforeEach
    void setUp() {
        orderDeliveryService = new OrderDeliveryService(
                snsPublisher,
                ORDER_READY_FOR_DELIVERY_TOPIC_ARN
        );
        
        // Setup captors for SNS requests
        topicArnCaptor = ArgumentCaptor.forClass(String.class);
        messageCaptor = ArgumentCaptor.forClass(String.class);
    }

    @Test
    void shouldPrepareOrderForDeliverySuccessfully() {
        // Given
        PaymentCompletedEvent event = createTestPaymentCompletedEvent();
        doNothing().when(snsPublisher).publishMessage(anyString(), anyString());

        // When
        orderDeliveryService.prepareOrderForDelivery(event);

        // Then
        verify(snsPublisher).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Verify SNS request content
        String topicArn = topicArnCaptor.getValue();
        String message = messageCaptor.getValue();
        
        assertThat(topicArn).isEqualTo(ORDER_READY_FOR_DELIVERY_TOPIC_ARN);
        assertThat(message).contains("orderId");
        assertThat(message).contains("orderNumber");
        assertThat(message).contains("customerEmail");
        assertThat(message).contains("shippingAddress");
        assertThat(message).contains("parcelLength");
        assertThat(message).contains("parcelWeight");
        
        // Verify specific order data
        assertThat(message).contains("order-123");
        assertThat(message).contains("ORD-123");
        assertThat(message).contains("john.doe@example.com");
        assertThat(message).contains("123 Main St");
        assertThat(message).contains("100.0");
        assertThat(message).contains("USD");
    }

    @Test
    void shouldHandleSnsPublishError() {
        // Given
        PaymentCompletedEvent event = createTestPaymentCompletedEvent();
        
        doThrow(new RuntimeException("SNS publish failed"))
                .when(snsPublisher).publishMessage(anyString(), anyString());

        // When & Then
        assertThatThrownBy(() -> orderDeliveryService.prepareOrderForDelivery(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to prepare order for delivery");
    }

    @Test
    void shouldHandleNullEvent() {
        // When & Then
        assertThatThrownBy(() -> orderDeliveryService.prepareOrderForDelivery(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldHandleEventWithNullFields() {
        // Given
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        // Other fields are null
        doNothing().when(snsPublisher).publishMessage(anyString(), anyString());

        // When
        orderDeliveryService.prepareOrderForDelivery(event);

        // Then
        verify(snsPublisher).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Verify SNS request content
        String topicArn = topicArnCaptor.getValue();
        String message = messageCaptor.getValue();
        
        assertThat(topicArn).isEqualTo(ORDER_READY_FOR_DELIVERY_TOPIC_ARN);
        assertThat(message).contains("order-123");
        assertThat(message).contains("ORD-123");
        // Should handle null fields gracefully
        assertThat(message).contains("null");
    }

    @Test
    void shouldHandleLargeOrder() {
        // Given
        PaymentCompletedEvent event = createLargeOrderEvent();
        doNothing().when(snsPublisher).publishMessage(anyString(), anyString());

        // When
        orderDeliveryService.prepareOrderForDelivery(event);

        // Then
        verify(snsPublisher).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Verify SNS request content
        String topicArn = topicArnCaptor.getValue();
        String message = messageCaptor.getValue();
        
        assertThat(topicArn).isEqualTo(ORDER_READY_FOR_DELIVERY_TOPIC_ARN);
        assertThat(message).contains("9999.99");
        assertThat(message).contains("20.0");
        assertThat(message).contains("15.0");
        assertThat(message).contains("10.0");
        assertThat(message).contains("25.0");
    }

    @Test
    void shouldHandleOrderWithSpecialCharacters() {
        // Given
        PaymentCompletedEvent event = createTestPaymentCompletedEvent();
        event.setOrderNumber("ORD-123-特殊字符-@#$%");
        doNothing().when(snsPublisher).publishMessage(anyString(), anyString());

        // When
        orderDeliveryService.prepareOrderForDelivery(event);

        // Then
        verify(snsPublisher).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Verify SNS request content
        String topicArn = topicArnCaptor.getValue();
        String message = messageCaptor.getValue();
        
        assertThat(topicArn).isEqualTo(ORDER_READY_FOR_DELIVERY_TOPIC_ARN);
        assertThat(message).contains("ORD-123-特殊字符-@#$%");
    }

    @Test
    void shouldHandleEventWithZeroParcelDimensions() {
        // Given
        PaymentCompletedEvent event = createTestPaymentCompletedEvent();
        event.setParcelLength(0.0);
        event.setParcelWidth(0.0);
        event.setParcelHeight(0.0);
        event.setParcelWeight(0.0);
        doNothing().when(snsPublisher).publishMessage(anyString(), anyString());

        // When
        orderDeliveryService.prepareOrderForDelivery(event);

        // Then
        verify(snsPublisher).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Verify SNS request content
        String topicArn = topicArnCaptor.getValue();
        String message = messageCaptor.getValue();
        
        assertThat(topicArn).isEqualTo(ORDER_READY_FOR_DELIVERY_TOPIC_ARN);
        assertThat(message).contains("0.0");
    }

    @Test
    void shouldHandleEventWithNegativeParcelDimensions() {
        // Given
        PaymentCompletedEvent event = createTestPaymentCompletedEvent();
        event.setParcelLength(-10.0);
        event.setParcelWidth(-5.0);
        event.setParcelHeight(-2.0);
        event.setParcelWeight(-1.5);
        doNothing().when(snsPublisher).publishMessage(anyString(), anyString());

        // When
        orderDeliveryService.prepareOrderForDelivery(event);

        // Then
        verify(snsPublisher).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Verify SNS request content
        String topicArn = topicArnCaptor.getValue();
        String message = messageCaptor.getValue();
        
        assertThat(topicArn).isEqualTo(ORDER_READY_FOR_DELIVERY_TOPIC_ARN);
        assertThat(message).contains("-10.0");
        assertThat(message).contains("-5.0");
        assertThat(message).contains("-2.0");
        assertThat(message).contains("-1.5");
    }

    @Test
    void shouldHandleEventWithMissingShippingInfo() {
        // Given
        PaymentCompletedEvent event = createTestPaymentCompletedEvent();
        event.setShippingAddress(null);
        event.setShippingCity(null);
        event.setShippingState(null);
        event.setShippingZip(null);
        event.setShippingCountry(null);
        doNothing().when(snsPublisher).publishMessage(anyString(), anyString());

        // When
        orderDeliveryService.prepareOrderForDelivery(event);

        // Then
        verify(snsPublisher).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Verify SNS request content
        String topicArn = topicArnCaptor.getValue();
        String message = messageCaptor.getValue();
        
        assertThat(topicArn).isEqualTo(ORDER_READY_FOR_DELIVERY_TOPIC_ARN);
        assertThat(message).contains("order-123");
        assertThat(message).contains("ORD-123");
        // Should handle null shipping info gracefully
        assertThat(message).contains("null");
    }

    @Test
    void shouldHandleEventWithMissingParcelInfo() {
        // Given
        PaymentCompletedEvent event = createTestPaymentCompletedEvent();
        event.setParcelLength(null);
        event.setParcelWidth(null);
        event.setParcelHeight(null);
        event.setParcelWeight(null);
        event.setParcelDistanceUnit(null);
        event.setParcelMassUnit(null);
        doNothing().when(snsPublisher).publishMessage(anyString(), anyString());

        // When
        orderDeliveryService.prepareOrderForDelivery(event);

        // Then
        verify(snsPublisher).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Verify SNS request content
        String topicArn = topicArnCaptor.getValue();
        String message = messageCaptor.getValue();
        
        assertThat(topicArn).isEqualTo(ORDER_READY_FOR_DELIVERY_TOPIC_ARN);
        assertThat(message).contains("order-123");
        assertThat(message).contains("ORD-123");
        // Should handle null parcel info gracefully
        assertThat(message).contains("null");
    }

    private PaymentCompletedEvent createTestPaymentCompletedEvent() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        event.setCustomerEmail("john.doe@example.com");
        event.setCustomerName("John Doe");
        event.setShippingAddress("123 Main St");
        event.setShippingCity("New York");
        event.setShippingState("NY");
        event.setShippingZip("10001");
        event.setShippingCountry("US");
        event.setTotalAmount(100.0);
        event.setCurrency("USD");
        event.setParcelLength(10.0);
        event.setParcelWidth(5.0);
        event.setParcelHeight(2.0);
        event.setParcelDistanceUnit("cm");
        event.setParcelWeight(1.5);
        event.setParcelMassUnit("kg");
        return event;
    }

    private PaymentCompletedEvent createLargeOrderEvent() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId("order-456");
        event.setOrderNumber("ORD-456");
        event.setCustomerEmail("jane.smith@example.com");
        event.setCustomerName("Jane Smith");
        event.setShippingAddress("456 Oak Ave");
        event.setShippingCity("Los Angeles");
        event.setShippingState("CA");
        event.setShippingZip("90210");
        event.setShippingCountry("US");
        event.setTotalAmount(9999.99);
        event.setCurrency("USD");
        event.setParcelLength(20.0);
        event.setParcelWidth(15.0);
        event.setParcelHeight(10.0);
        event.setParcelDistanceUnit("cm");
        event.setParcelWeight(25.0);
        event.setParcelMassUnit("kg");
        return event;
    }
} 
package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.OrderReadyForDeliveryEvent;
import com.ecommerce.lambda.model.OrderStatusUpdateEvent;
import com.ecommerce.lambda.model.PaymentCompletedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private EmailService emailService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(emailService);
    }

    @Test
    void shouldProcessPaymentCompletedSuccessfully() {
        // Given
        PaymentCompletedEvent event = createValidPaymentCompletedEvent();
        doNothing().when(emailService).sendPaymentCompletedEmail(event);

        // When
        notificationService.processPaymentCompleted(event);

        // Then
        verify(emailService).sendPaymentCompletedEmail(event);
    }

    @Test
    void shouldThrowExceptionForNullPaymentCompletedEvent() {
        // When & Then
        assertThatThrownBy(() -> notificationService.processPaymentCompleted(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process payment completed notification");
    }

    @Test
    void shouldThrowExceptionForInvalidPaymentCompletedEvent() {
        // Given
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderNumber(null); // Invalid

        // When & Then
        assertThatThrownBy(() -> notificationService.processPaymentCompleted(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process payment completed notification");
    }

    @Test
    void shouldThrowExceptionForInvalidEmail() {
        // Given
        PaymentCompletedEvent event = createValidPaymentCompletedEvent();
        event.setCustomerEmail("invalid-email"); // Invalid email format

        // When & Then
        assertThatThrownBy(() -> notificationService.processPaymentCompleted(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process payment completed notification");
    }

    @Test
    void shouldProcessOrderReadyForDeliverySuccessfully() {
        // Given
        OrderReadyForDeliveryEvent event = createValidOrderReadyForDeliveryEvent();
        doNothing().when(emailService).sendOrderReadyForDeliveryEmail(event);

        // When
        notificationService.processOrderReadyForDelivery(event);

        // Then
        verify(emailService).sendOrderReadyForDeliveryEmail(event);
    }

    @Test
    void shouldThrowExceptionForNullOrderReadyForDeliveryEvent() {
        // When & Then
        assertThatThrownBy(() -> notificationService.processOrderReadyForDelivery(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order ready for delivery notification");
    }

    @Test
    void shouldProcessOrderStatusUpdatedSuccessfully() {
        // Given
        OrderStatusUpdateEvent event = createValidOrderStatusUpdateEvent();
        doNothing().when(emailService).sendOrderStatusUpdatedEmail(event);

        // When
        notificationService.processOrderStatusUpdated(event);

        // Then
        verify(emailService).sendOrderStatusUpdatedEmail(event);
    }

    @Test
    void shouldThrowExceptionForNullOrderStatusUpdateEvent() {
        // When & Then
        assertThatThrownBy(() -> notificationService.processOrderStatusUpdated(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order status updated notification");
    }

    @Test
    void shouldHandleEmailServiceException() {
        // Given
        PaymentCompletedEvent event = createValidPaymentCompletedEvent();
        doThrow(new RuntimeException("Email service error"))
                .when(emailService).sendPaymentCompletedEmail(event);

        // When & Then
        assertThatThrownBy(() -> notificationService.processPaymentCompleted(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process payment completed notification");
    }

    @Test
    void shouldValidateEmptyOrderNumber() {
        // Given
        PaymentCompletedEvent event = createValidPaymentCompletedEvent();
        event.setOrderNumber(""); // Empty order number

        // When & Then
        assertThatThrownBy(() -> notificationService.processPaymentCompleted(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process payment completed notification");
    }

    @Test
    void shouldValidateEmptyCustomerEmail() {
        // Given
        PaymentCompletedEvent event = createValidPaymentCompletedEvent();
        event.setCustomerEmail(""); // Empty email

        // When & Then
        assertThatThrownBy(() -> notificationService.processPaymentCompleted(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process payment completed notification");
    }

    @Test
    void shouldValidateEmptyCustomerName() {
        // Given
        PaymentCompletedEvent event = createValidPaymentCompletedEvent();
        event.setCustomerName(""); // Empty name

        // When & Then
        assertThatThrownBy(() -> notificationService.processPaymentCompleted(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process payment completed notification");
    }

    @Test
    void shouldValidateNullShippingAddress() {
        // Given
        OrderReadyForDeliveryEvent event = createValidOrderReadyForDeliveryEvent();
        event.setShippingAddress(null); // Null shipping address

        // When & Then
        assertThatThrownBy(() -> notificationService.processOrderReadyForDelivery(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order ready for delivery notification");
    }

    @Test
    void shouldValidateEmptyStatus() {
        // Given
        OrderStatusUpdateEvent event = createValidOrderStatusUpdateEvent();
        event.setStatus(""); // Empty status

        // When & Then
        assertThatThrownBy(() -> notificationService.processOrderStatusUpdated(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order status updated notification");
    }

    @Test
    void shouldProcessShippingInitiated() {
        // Given
        OrderStatusUpdateEvent event = new OrderStatusUpdateEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        event.setCustomerEmail("customer@example.com");
        event.setCustomerName("John Doe");
        event.setStatus("SHIPPING_INITIATED");
        event.setTrackingNumber("TRACK123");

        // When
        notificationService.processShippingInitiated(event);

        // Then
        verify(emailService).sendOrderReadyForDeliveryEmail(any(OrderReadyForDeliveryEvent.class));
    }

    @Test
    void shouldThrowExceptionForNullEventInProcessShippingInitiated() {
        // When & Then
        assertThatThrownBy(() -> notificationService.processShippingInitiated(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process shipping initiated notification");
    }

    @Test
    void shouldThrowExceptionWhenEmailServiceFailsInProcessShippingInitiated() {
        // Given
        OrderStatusUpdateEvent event = new OrderStatusUpdateEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        event.setCustomerEmail("customer@example.com");
        event.setCustomerName("John Doe");
        event.setStatus("SHIPPING_INITIATED");

        doThrow(new RuntimeException("Email service error"))
                .when(emailService).sendOrderReadyForDeliveryEmail(any(OrderReadyForDeliveryEvent.class));

        // When & Then
        assertThatThrownBy(() -> notificationService.processShippingInitiated(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process shipping initiated notification");
    }

    @Test
    void shouldProcessShippingError() {
        // Given
        OrderStatusUpdateEvent event = new OrderStatusUpdateEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        event.setCustomerEmail("customer@example.com");
        event.setCustomerName("John Doe");
        event.setStatus("SHIPPING_FAILED");
        event.setNotes("Shippo API error: Address validation failed");

        // When
        notificationService.processShippingError(event);

        // Then
        verify(emailService).sendOrderStatusUpdatedEmail(event);
    }

    @Test
    void shouldThrowExceptionForNullEventInProcessShippingError() {
        // When & Then
        assertThatThrownBy(() -> notificationService.processShippingError(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process shipping error notification");
    }

    @Test
    void shouldThrowExceptionWhenEmailServiceFailsInProcessShippingError() {
        // Given
        OrderStatusUpdateEvent event = new OrderStatusUpdateEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        event.setCustomerEmail("customer@example.com");
        event.setCustomerName("John Doe");
        event.setStatus("SHIPPING_FAILED");

        doThrow(new RuntimeException("Email service error"))
                .when(emailService).sendOrderStatusUpdatedEmail(any(OrderStatusUpdateEvent.class));

        // When & Then
        assertThatThrownBy(() -> notificationService.processShippingError(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process shipping error notification");
    }

    @Test
    void shouldConvertOrderStatusUpdateEventToDeliveryEvent() {
        // Given
        OrderStatusUpdateEvent statusEvent = new OrderStatusUpdateEvent();
        statusEvent.setOrderId("order-123");
        statusEvent.setOrderNumber("ORD-123");
        statusEvent.setCustomerEmail("customer@example.com");
        statusEvent.setCustomerName("John Doe");
        statusEvent.setStatus("SHIPPING_INITIATED");

        // When
        notificationService.processShippingInitiated(statusEvent);

        // Then
        verify(emailService).sendOrderReadyForDeliveryEmail(argThat(deliveryEvent -> 
            deliveryEvent.getOrderId().equals("order-123") &&
            deliveryEvent.getOrderNumber().equals("ORD-123") &&
            deliveryEvent.getCustomerEmail().equals("customer@example.com") &&
            deliveryEvent.getCustomerName().equals("John Doe")
        ));
    }

    private PaymentCompletedEvent createValidPaymentCompletedEvent() {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        event.setCustomerEmail("customer@example.com");
        event.setCustomerName("John Doe");
        event.setPaymentId("payment-123");
        event.setTotalAmount(100.0);
        event.setCurrency("USD");
        return event;
    }

    private OrderReadyForDeliveryEvent createValidOrderReadyForDeliveryEvent() {
        OrderReadyForDeliveryEvent event = new OrderReadyForDeliveryEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        event.setCustomerEmail("customer@example.com");
        event.setCustomerName("John Doe");
        
        // Устанавливаем отдельные поля адреса
        event.setShippingAddress("123 Main St");
        event.setShippingCity("New York");
        event.setShippingState("NY");
        event.setShippingZip("10001");
        event.setShippingCountry("US");
        
        return event;
    }

    private OrderStatusUpdateEvent createValidOrderStatusUpdateEvent() {
        OrderStatusUpdateEvent event = new OrderStatusUpdateEvent();
        event.setOrderId("order-123");
        event.setOrderNumber("ORD-123");
        event.setCustomerEmail("customer@example.com");
        event.setCustomerName("John Doe");
        event.setStatus("DELIVERED");
        event.setUpdatedAt("2023-12-01T10:00:00Z");
        return event;
    }
} 
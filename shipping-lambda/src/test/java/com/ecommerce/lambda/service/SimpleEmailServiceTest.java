package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.OrderReadyForDeliveryEvent;
import com.ecommerce.lambda.model.OrderStatusUpdateEvent;
import com.ecommerce.lambda.model.PaymentCompletedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleEmailServiceTest {

    @Mock
    private SesV2Client sesClient;

    private SimpleEmailService emailService;
    private final String fromEmail = "noreply@example.com";

    @BeforeEach
    void setUp() {
        emailService = new SimpleEmailService(sesClient, fromEmail);
    }

    @Test
    void shouldSendPaymentCompletedEmailSuccessfully() {
        // Given
        PaymentCompletedEvent event = createPaymentCompletedEvent();
        SendEmailResponse response = SendEmailResponse.builder()
                .messageId("message-123")
                .build();
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(response);

        // When
        emailService.sendPaymentCompletedEmail(event);

        // Then
        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(requestCaptor.capture());
        
        SendEmailRequest request = requestCaptor.getValue();
        assertThat(request.fromEmailAddress()).isEqualTo(fromEmail);
        assertThat(request.destination().toAddresses()).contains("customer@example.com");
        assertThat(request.content().simple().subject().data()).contains("ORD-123");
        assertThat(request.content().simple().body().text().data()).contains("John Doe");
        assertThat(request.content().simple().body().text().data()).contains("100,00 USD");
    }

    @Test
    void shouldSendOrderReadyForDeliveryEmailSuccessfully() {
        // Given
        OrderReadyForDeliveryEvent event = createOrderReadyForDeliveryEvent();
        SendEmailResponse response = SendEmailResponse.builder()
                .messageId("message-123")
                .build();
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(response);

        // When
        emailService.sendOrderReadyForDeliveryEmail(event);

        // Then
        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(requestCaptor.capture());
        
        SendEmailRequest request = requestCaptor.getValue();
        assertThat(request.fromEmailAddress()).isEqualTo(fromEmail);
        assertThat(request.destination().toAddresses()).contains("customer@example.com");
        assertThat(request.content().simple().subject().data()).contains("ORD-123");
        assertThat(request.content().simple().body().text().data()).contains("ready for shipment");
        assertThat(request.content().simple().body().text().data()).contains("123 Main St");
    }

    @Test
    void shouldSendOrderStatusUpdatedEmailSuccessfully() {
        // Given
        OrderStatusUpdateEvent event = createOrderStatusUpdateEvent();
        SendEmailResponse response = SendEmailResponse.builder()
                .messageId("message-123")
                .build();
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(response);

        // When
        emailService.sendOrderStatusUpdatedEmail(event);

        // Then
        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(requestCaptor.capture());
        
        SendEmailRequest request = requestCaptor.getValue();
        assertThat(request.fromEmailAddress()).isEqualTo(fromEmail);
        assertThat(request.destination().toAddresses()).contains("customer@example.com");
        assertThat(request.content().simple().subject().data()).contains("ORD-123");
        assertThat(request.content().simple().body().text().data()).contains("Delivered");
    }

    @Test
    void shouldHandleSesException() {
        // Given
        PaymentCompletedEvent event = createPaymentCompletedEvent();
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(SesV2Exception.builder().message("SES error").build());

        // When & Then
        assertThatThrownBy(() -> emailService.sendPaymentCompletedEmail(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send payment completed email");
    }

    @Test
    void shouldHandleGenericException() {
        // Given
        OrderReadyForDeliveryEvent event = createOrderReadyForDeliveryEvent();
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(new RuntimeException("Generic error"));

        // When & Then
        assertThatThrownBy(() -> emailService.sendOrderReadyForDeliveryEmail(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send order ready email");
    }

    @Test
    void shouldUseCorrectStatusDisplayNames() {
        // Given
        OrderStatusUpdateEvent event = createOrderStatusUpdateEvent();
        event.setStatus("PAYMENT_COMPLETED");
        
        SendEmailResponse response = SendEmailResponse.builder()
                .messageId("message-123")
                .build();
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(response);

        // When
        emailService.sendOrderStatusUpdatedEmail(event);

        // Then
        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(requestCaptor.capture());
        
        SendEmailRequest request = requestCaptor.getValue();
        assertThat(request.content().simple().body().text().data()).contains("Paid");
    }

    @Test
    void shouldHandleDifferentStatusTypes() {
        // Given
        OrderStatusUpdateEvent event = createOrderStatusUpdateEvent();
        event.setStatus("IN_DELIVERY");
        
        SendEmailResponse response = SendEmailResponse.builder()
                .messageId("message-123")
                .build();
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(response);

        // When
        emailService.sendOrderStatusUpdatedEmail(event);

        // Then
        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(requestCaptor.capture());
        
        SendEmailRequest request = requestCaptor.getValue();
        assertThat(request.content().simple().body().text().data()).contains("In Transit");
        assertThat(request.content().simple().body().text().data()).contains("Track the status using the tracking number");
    }

    @Test
    void shouldHandleUnknownStatus() {
        // Given
        OrderStatusUpdateEvent event = createOrderStatusUpdateEvent();
        event.setStatus("UNKNOWN_STATUS");
        
        SendEmailResponse response = SendEmailResponse.builder()
                .messageId("message-123")
                .build();
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(response);

        // When
        emailService.sendOrderStatusUpdatedEmail(event);

        // Then
        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(requestCaptor.capture());
        
        SendEmailRequest request = requestCaptor.getValue();
        assertThat(request.content().simple().body().text().data()).contains("UNKNOWN_STATUS");
        assertThat(request.content().simple().body().text().data()).contains("Your order status has been updated");
    }

    private PaymentCompletedEvent createPaymentCompletedEvent() {
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

    private OrderReadyForDeliveryEvent createOrderReadyForDeliveryEvent() {
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

    private OrderStatusUpdateEvent createOrderStatusUpdateEvent() {
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
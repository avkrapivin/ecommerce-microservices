package com.ecommerce.lambda.service;

import com.ecommerce.lambda.TestDataFactory;
import com.ecommerce.lambda.model.Order;
import com.ecommerce.lambda.model.PaymentCompletedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.Payer;
import com.paypal.api.payments.PayerInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderDispatcherServiceTest {

    @Mock
    private SnsPublisher snsPublisher;

    @Mock
    private PayPalService payPalService;

    private OrderDispatcherService orderDispatcherService;
    private ArgumentCaptor<String> topicArnCaptor;
    private ArgumentCaptor<String> messageCaptor;
    private ObjectMapper objectMapper;

    private static final String PAYMENT_COMPLETED_TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:payment-completed";
    private static final String PAYMENT_SUSPICIOUS_TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:payment-suspicious";
    private static final String ORDER_STATUS_UPDATED_TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:order-status-updated";

    @BeforeEach
    void setUp() {
        orderDispatcherService = new OrderDispatcherService(
                snsPublisher,
                payPalService,
                PAYMENT_COMPLETED_TOPIC_ARN,
                PAYMENT_SUSPICIOUS_TOPIC_ARN,
                ORDER_STATUS_UPDATED_TOPIC_ARN
        );
        topicArnCaptor = ArgumentCaptor.forClass(String.class);
        messageCaptor = ArgumentCaptor.forClass(String.class);
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldProcessOrderSuccessfully() throws Exception {
        // Given
        Order order = TestDataFactory.createTestOrder();
        Payment mockPayment = createMockPayment("payment-123", "approved");
        
        when(payPalService.createOrder(anyDouble(), anyString(), anyString())).thenReturn(mockPayment);
        when(payPalService.captureOrder(anyString(), anyString())).thenReturn(mockPayment);
        when(payPalService.isOrderApproved(any(Payment.class))).thenReturn(true);
        doNothing().when(snsPublisher).publishMessage(anyString(), anyString());

        // When
        orderDispatcherService.processOrder(order);

        // Then
        verify(payPalService).createOrder(100.0, "USD", "ORD-123");
        verify(payPalService).captureOrder("payment-123", "payer-123");
        verify(payPalService).isOrderApproved(mockPayment);
        
        // Проверяем количество вызовов SNS (2 вызова: payment-completed и order-status-updated)
        verify(snsPublisher, times(2)).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Проверяем содержимое SNS сообщений
        var capturedTopicArns = topicArnCaptor.getAllValues();
        var capturedMessages = messageCaptor.getAllValues();
        
        // Первое сообщение - PaymentCompletedEvent
        assertThat(capturedTopicArns.get(0)).isEqualTo(PAYMENT_COMPLETED_TOPIC_ARN);
        
        PaymentCompletedEvent paymentEvent = objectMapper.readValue(
            capturedMessages.get(0), 
            PaymentCompletedEvent.class
        );
        assertThat(paymentEvent.getOrderId()).isEqualTo("order-123");
        assertThat(paymentEvent.getOrderNumber()).isEqualTo("ORD-123");
        assertThat(paymentEvent.getPaymentId()).isEqualTo("payment-123");
        assertThat(paymentEvent.getPaymentStatus()).isEqualTo("approved");
        assertThat(paymentEvent.getPaymentMethod()).isEqualTo("PAYPAL");
        assertThat(paymentEvent.getCustomerEmail()).isEqualTo("customer-123");
        assertThat(paymentEvent.getShippingAddress()).isEqualTo("123 Main St");
        assertThat(paymentEvent.getShippingCity()).isEqualTo("New York");
        assertThat(paymentEvent.getTotalAmount()).isEqualTo(100.0);
        assertThat(paymentEvent.getCurrency()).isEqualTo("USD");
        assertThat(paymentEvent.getParcelLength()).isGreaterThan(0);
        assertThat(paymentEvent.getParcelWeight()).isGreaterThan(0);
        
        // Второе сообщение - OrderStatusUpdate
        assertThat(capturedTopicArns.get(1)).isEqualTo(ORDER_STATUS_UPDATED_TOPIC_ARN);
        assertThat(capturedMessages.get(1)).contains("\"status\":\"PAYMENT_COMPLETED\"");
    }

    @Test
    void shouldHandleFailedPayment() throws Exception {
        // Given
        Order order = TestDataFactory.createTestOrder();
        Payment mockPayment = createMockPaymentForFailedScenario("payment-123", "failed");
        
        when(payPalService.createOrder(anyDouble(), anyString(), anyString())).thenReturn(mockPayment);
        when(payPalService.captureOrder(anyString(), anyString())).thenReturn(mockPayment);
        when(payPalService.isOrderApproved(any(Payment.class))).thenReturn(false);
        doNothing().when(snsPublisher).publishMessage(anyString(), anyString());

        // When
        orderDispatcherService.processOrder(order);

        // Then
        verify(payPalService).createOrder(100.0, "USD", "ORD-123");
        verify(payPalService).captureOrder("payment-123", "payer-123");
        verify(payPalService).isOrderApproved(mockPayment);
        
        // Проверяем количество вызовов SNS (2 вызова: payment-suspicious и order-status-updated)
        verify(snsPublisher, times(2)).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Проверяем содержимое SNS сообщений
        var capturedTopicArns = topicArnCaptor.getAllValues();
        var capturedMessages = messageCaptor.getAllValues();
        
        // Первое сообщение - подозрительный платеж
        assertThat(capturedTopicArns.get(0)).isEqualTo(PAYMENT_SUSPICIOUS_TOPIC_ARN);
        assertThat(capturedMessages.get(0)).contains("order-123");
        
        // Второе сообщение - OrderStatusUpdate с PAYMENT_FAILED
        assertThat(capturedTopicArns.get(1)).isEqualTo(ORDER_STATUS_UPDATED_TOPIC_ARN);
        assertThat(capturedMessages.get(1)).contains("\"status\":\"PAYMENT_FAILED\"");
    }

    @Test
    void shouldHandleNullOrder() {
        // When & Then
        assertThatThrownBy(() -> orderDispatcherService.processOrder(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldHandleOrderWithNullTotal() {
        // Given
        Order order = TestDataFactory.createTestOrder();
        order.setTotal(null);

        // When & Then
        assertThatThrownBy(() -> orderDispatcherService.processOrder(order))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order");
    }

    @Test
    void shouldHandleOrderWithNullOrderNumber() {
        // Given
        Order order = TestDataFactory.createTestOrder();
        order.setOrderNumber(null);

        // When & Then
        assertThatThrownBy(() -> orderDispatcherService.processOrder(order))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order");
    }

    @Test
    void shouldHandleSnsPublishError() throws Exception {
        // Given
        Order order = TestDataFactory.createTestOrder();
        
        // Mock PayPal service responses
        Payment mockPayment = createMockPayment("payment-123", "approved");
        when(payPalService.createOrder(anyDouble(), anyString(), anyString())).thenReturn(mockPayment);
        when(payPalService.captureOrder(anyString(), anyString())).thenReturn(mockPayment);
        when(payPalService.isOrderApproved(any(Payment.class))).thenReturn(true);
        
        doThrow(new RuntimeException("SNS publish failed"))
                .when(snsPublisher).publishMessage(anyString(), anyString());

        // When & Then
        assertThatThrownBy(() -> orderDispatcherService.processOrder(order))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order");
    }

    @Test
    void shouldHandleOrderWithoutItems() throws Exception {
        // Given
        Order order = TestDataFactory.createTestOrder();
        order.setItems(null);

        // When & Then
        assertThatThrownBy(() -> orderDispatcherService.processOrder(order))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order");
    }

    @Test
    void shouldCalculateParcelDimensionsCorrectly() throws Exception {
        // Given
        Order order = TestDataFactory.createTestOrder();
        Payment mockPayment = createMockPayment("payment-123", "approved");
        
        when(payPalService.createOrder(anyDouble(), anyString(), anyString())).thenReturn(mockPayment);
        when(payPalService.captureOrder(anyString(), anyString())).thenReturn(mockPayment);
        when(payPalService.isOrderApproved(any(Payment.class))).thenReturn(true);
        doNothing().when(snsPublisher).publishMessage(anyString(), anyString());

        // When
        orderDispatcherService.processOrder(order);

        // Then
        verify(snsPublisher, times(2)).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Проверяем, что размеры посылки рассчитаны корректно
        var capturedMessages = messageCaptor.getAllValues();
        PaymentCompletedEvent paymentEvent = objectMapper.readValue(
            capturedMessages.get(0), 
            PaymentCompletedEvent.class
        );
        
        // Проверяем, что размеры посылки больше нуля
        assertThat(paymentEvent.getParcelLength()).isGreaterThan(0);
        assertThat(paymentEvent.getParcelWidth()).isGreaterThan(0);
        assertThat(paymentEvent.getParcelHeight()).isGreaterThan(0);
        assertThat(paymentEvent.getParcelWeight()).isGreaterThan(0);
        
        // Проверяем единицы измерения
        assertThat(paymentEvent.getParcelDistanceUnit()).isEqualTo("cm");
        assertThat(paymentEvent.getParcelMassUnit()).isEqualTo("kg");
    }

    private Payment createMockPayment(String paymentId, String state) {
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setState(state);
        payment.setCreateTime("2023-01-01T00:00:00Z");
        
        Payer payer = new Payer();
        
        PayerInfo payerInfo = new PayerInfo();
        payerInfo.setPayerId("payer-123");
        payer.setPayerInfo(payerInfo);
        
        payment.setPayer(payer);
        return payment;
    }

    private Payment createMockPaymentForFailedScenario(String paymentId, String state) {
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setState(state);
        payment.setCreateTime("2023-01-01T00:00:00Z");
        
        Payer payer = new Payer();
        
        PayerInfo payerInfo = new PayerInfo();
        payerInfo.setPayerId("payer-123");
        payer.setPayerInfo(payerInfo);
        
        payment.setPayer(payer);
        return payment;
    }
} 
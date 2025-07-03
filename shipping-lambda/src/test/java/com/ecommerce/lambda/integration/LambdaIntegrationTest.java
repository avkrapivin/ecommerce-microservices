package com.ecommerce.lambda.integration;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.ecommerce.lambda.OrderDispatcherHandler;
import com.ecommerce.lambda.TestDataFactory;
import com.ecommerce.lambda.model.Order;
import com.ecommerce.lambda.service.OrderDispatcherService;
import com.ecommerce.lambda.service.PayPalService;
import com.ecommerce.lambda.service.SnsPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.Payer;
import com.paypal.api.payments.PayerInfo;
import com.paypal.base.rest.PayPalRESTException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Testcontainers
class LambdaIntegrationTest {

    @Mock
    private SnsPublisher snsPublisher;

    @Mock
    private PayPalService payPalService;

    private Context context;
    private OrderDispatcherHandler handler;
    private OrderDispatcherService orderDispatcherService;
    private ObjectMapper objectMapper;
    private ArgumentCaptor<String> topicArnCaptor;
    private ArgumentCaptor<String> messageCaptor;

    private static final String PAYMENT_COMPLETED_TOPIC_ARN = "arn:aws:sns:us-east-1:000000000000:payment-completed";
    private static final String PAYMENT_SUSPICIOUS_TOPIC_ARN = "arn:aws:sns:us-east-1:000000000000:payment-suspicious";
    private static final String ORDER_STATUS_UPDATED_TOPIC_ARN = "arn:aws:sns:us-east-1:000000000000:order-status-updated";

    @BeforeEach
    void setUp() {
        // Create services
        orderDispatcherService = new OrderDispatcherService(
                snsPublisher,
                payPalService,
                PAYMENT_COMPLETED_TOPIC_ARN,
                PAYMENT_SUSPICIOUS_TOPIC_ARN,
                ORDER_STATUS_UPDATED_TOPIC_ARN
        );

        objectMapper = new ObjectMapper();
        handler = new OrderDispatcherHandler(orderDispatcherService, objectMapper);
        
        // Create a simple mock context
        context = mock(Context.class);
        
        // Setup captors for SNS requests
        topicArnCaptor = ArgumentCaptor.forClass(String.class);
        messageCaptor = ArgumentCaptor.forClass(String.class);
    }

    @Test
    void shouldProcessOrderWithLocalStackSns() throws Exception {
        // Given
        Order order = TestDataFactory.createTestOrder();
        String orderJson = TestDataFactory.orderToJson(order);
        SNSEvent event = TestDataFactory.createSnsEvent(orderJson);
        
        // Setup mocks
        setupPayPalMocks("payment-123", "approved");
        setupSnsMock();

        // When
        handler.handleRequest(event, context);

        // Then
        // Verify that SNS publish was called 2 times (payment completed + order status)
        verify(snsPublisher, times(2)).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Verify SNS messages content
        List<String> topicArns = topicArnCaptor.getAllValues();
        List<String> messages = messageCaptor.getAllValues();
        assertThat(topicArns).hasSize(2);
        assertThat(messages).hasSize(2);
        
        // First message should be payment completed event
        assertThat(topicArns.get(0)).isEqualTo(PAYMENT_COMPLETED_TOPIC_ARN);
        assertThat(messages.get(0)).contains("orderId");
        assertThat(messages.get(0)).contains("paymentId");
        assertThat(messages.get(0)).contains("PAYPAL");
        
        // Second message should be order status update
        assertThat(topicArns.get(1)).isEqualTo(ORDER_STATUS_UPDATED_TOPIC_ARN);
        assertThat(messages.get(1)).contains("PAYMENT_COMPLETED");
    }

    @Test
    void shouldHandleMultipleOrdersInBatch() throws Exception {
        // Given
        Order order1 = TestDataFactory.createTestOrder();
        Order order2 = TestDataFactory.createTestOrder();
        order2.setId("order-456");
        order2.setOrderNumber("ORD-456");

        String order1Json = TestDataFactory.orderToJson(order1);
        String order2Json = TestDataFactory.orderToJson(order2);
        SNSEvent event = TestDataFactory.createSnsEventWithMultipleRecords(order1Json, order2Json);
        
        // Setup mocks
        setupPayPalMocks("payment-123", "approved");
        setupSnsMock();

        // When
        handler.handleRequest(event, context);

        // Then
        // Verify that SNS publish was called for both orders (2 times each = 4 total)
        verify(snsPublisher, times(4)).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Verify SNS messages content
        List<String> topicArns = topicArnCaptor.getAllValues();
        List<String> messages = messageCaptor.getAllValues();
        assertThat(topicArns).hasSize(4);
        assertThat(messages).hasSize(4);
        
        // Verify payment completed messages
        assertThat(topicArns.get(0)).isEqualTo(PAYMENT_COMPLETED_TOPIC_ARN);
        assertThat(topicArns.get(2)).isEqualTo(PAYMENT_COMPLETED_TOPIC_ARN);
        
        // Verify order status messages
        assertThat(topicArns.get(1)).isEqualTo(ORDER_STATUS_UPDATED_TOPIC_ARN);
        assertThat(topicArns.get(3)).isEqualTo(ORDER_STATUS_UPDATED_TOPIC_ARN);
    }

    @Test
    void shouldHandleLargeOrderAmount() throws Exception {
        // Given
        Order order = TestDataFactory.createLargeOrder();
        String orderJson = TestDataFactory.orderToJson(order);
        SNSEvent event = TestDataFactory.createSnsEvent(orderJson);
        
        // Setup mocks
        setupPayPalMocks("payment-large", "approved");
        setupSnsMock();

        // When
        handler.handleRequest(event, context);

        // Then
        // Verify that SNS publish was called
        verify(snsPublisher, times(2)).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Verify SNS messages content
        List<String> topicArns = topicArnCaptor.getAllValues();
        List<String> messages = messageCaptor.getAllValues();
        assertThat(topicArns).hasSize(2);
        assertThat(messages).hasSize(2);
        assertThat(topicArns.get(0)).isEqualTo(PAYMENT_COMPLETED_TOPIC_ARN);
        assertThat(topicArns.get(1)).isEqualTo(ORDER_STATUS_UPDATED_TOPIC_ARN);
    }

    @Test
    void shouldHandleOrderWithSpecialCharacters() throws Exception {
        // Given
        Order order = TestDataFactory.createOrderWithSpecialCharacters();
        String orderJson = TestDataFactory.orderToJson(order);
        SNSEvent event = TestDataFactory.createSnsEvent(orderJson);
        
        // Setup mocks
        setupPayPalMocks("payment-special", "approved");
        setupSnsMock();

        // When
        handler.handleRequest(event, context);

        // Then
        // Verify that SNS publish was called
        verify(snsPublisher, times(2)).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Verify SNS messages content
        List<String> topicArns = topicArnCaptor.getAllValues();
        List<String> messages = messageCaptor.getAllValues();
        assertThat(topicArns).hasSize(2);
        assertThat(messages).hasSize(2);
        assertThat(topicArns.get(0)).isEqualTo(PAYMENT_COMPLETED_TOPIC_ARN);
        assertThat(topicArns.get(1)).isEqualTo(ORDER_STATUS_UPDATED_TOPIC_ARN);
    }

    @Test
    void shouldHandleOrderWithZeroAmount() throws Exception {
        // Given
        Order order = TestDataFactory.createOrderWithZeroAmount();
        String orderJson = TestDataFactory.orderToJson(order);
        SNSEvent event = TestDataFactory.createSnsEvent(orderJson);
        
        // Setup mocks
        setupPayPalMocks("payment-zero", "approved");
        setupSnsMock();

        // When
        handler.handleRequest(event, context);

        // Then
        // Verify that SNS publish was called
        verify(snsPublisher, times(2)).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Verify SNS messages content
        List<String> topicArns = topicArnCaptor.getAllValues();
        List<String> messages = messageCaptor.getAllValues();
        assertThat(topicArns).hasSize(2);
        assertThat(messages).hasSize(2);
        assertThat(topicArns.get(0)).isEqualTo(PAYMENT_COMPLETED_TOPIC_ARN);
        assertThat(topicArns.get(1)).isEqualTo(ORDER_STATUS_UPDATED_TOPIC_ARN);
    }

    @Test
    void shouldHandleOrderWithoutAddress() throws Exception {
        // Given
        Order order = TestDataFactory.createOrderWithoutAddress();
        String orderJson = TestDataFactory.orderToJson(order);
        SNSEvent event = TestDataFactory.createSnsEvent(orderJson);
        
        // Setup mocks
        setupPayPalMocks("payment-no-address", "approved");
        setupSnsMock();

        // When
        handler.handleRequest(event, context);

        // Then
        // Verify that SNS publish was called
        verify(snsPublisher, times(2)).publishMessage(topicArnCaptor.capture(), messageCaptor.capture());
        
        // Verify SNS messages content
        List<String> topicArns = topicArnCaptor.getAllValues();
        List<String> messages = messageCaptor.getAllValues();
        assertThat(topicArns).hasSize(2);
        assertThat(messages).hasSize(2);
        assertThat(topicArns.get(0)).isEqualTo(PAYMENT_COMPLETED_TOPIC_ARN);
        assertThat(topicArns.get(1)).isEqualTo(ORDER_STATUS_UPDATED_TOPIC_ARN);
    }

    @Test
    void shouldHandleEmptySnsEvent() throws Exception {
        // Given
        SNSEvent event = new SNSEvent();
        event.setRecords(List.of());

        // When
        handler.handleRequest(event, context);

        // Then
        // Should handle empty event gracefully
        verifyNoInteractions(snsPublisher);
    }

    @Test
    void shouldHandleSnsEventWithNullMessage() throws Exception {
        // Given
        SNSEvent event = new SNSEvent();
        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessage(null);
        record.setSns(sns);
        event.setRecords(List.of(record));

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(event, context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order");
        
        verifyNoInteractions(snsPublisher);
    }

    @Test
    void shouldHandleSnsPublishError() throws Exception {
        // Given
        Order order = TestDataFactory.createTestOrder();
        String orderJson = TestDataFactory.orderToJson(order);
        SNSEvent event = TestDataFactory.createSnsEvent(orderJson);
        
        // Setup PayPal mocks
        setupPayPalMocks("payment-error", "approved");
        
        // Setup SNS to throw error
        doThrow(new RuntimeException("SNS publish failed"))
                .when(snsPublisher).publishMessage(anyString(), anyString());

        // When & Then
        assertThatThrownBy(() -> handler.handleRequest(event, context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process order");
    }

    private void setupPayPalMocks(String paymentId, String state) throws PayPalRESTException {
        Payment mockPayment = createMockPayment(paymentId, state);
        when(payPalService.createOrder(any(Double.class), anyString(), anyString())).thenReturn(mockPayment);
        when(payPalService.captureOrder(anyString(), anyString())).thenReturn(mockPayment);
        when(payPalService.isOrderApproved(any(Payment.class))).thenReturn(true);
    }

    private void setupSnsMock() {
        doNothing().when(snsPublisher).publishMessage(anyString(), anyString());
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
} 
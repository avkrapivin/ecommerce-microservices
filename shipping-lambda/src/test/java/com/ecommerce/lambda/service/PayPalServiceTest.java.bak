package com.ecommerce.lambda.service;

import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayPalServiceTest {

    @Mock
    private PayPalClient payPalClient;

    @Mock
    private Payment mockPayment;

    private PayPalService payPalService;

    @BeforeEach
    void setUp() {
        payPalService = new PayPalService(payPalClient);
    }

    @Test
    void shouldReturnTrueWhenOrderIsApproved() {
        // Given
        when(payPalClient.isOrderApproved(mockPayment)).thenReturn(true);

        // When
        boolean isApproved = payPalService.isOrderApproved(mockPayment);

        // Then
        assertThat(isApproved).isTrue();
    }

    @Test
    void shouldReturnFalseWhenOrderIsNotApproved() {
        // Given
        when(payPalClient.isOrderApproved(mockPayment)).thenReturn(false);

        // When
        boolean isApproved = payPalService.isOrderApproved(mockPayment);

        // Then
        assertThat(isApproved).isFalse();
    }

    @Test
    void shouldHandleNullPayment() {
        // Given
        when(payPalClient.isOrderApproved(null)).thenReturn(false);

        // When
        boolean isApproved = payPalService.isOrderApproved(null);

        // Then
        assertThat(isApproved).isFalse();
    }

    @Test
    void shouldCreateOrderSuccessfully() throws Exception {
        // Given
        double amount = 100.0;
        String currency = "USD";
        String orderNumber = "ORD-123";

        Payment expectedPayment = new Payment();
        expectedPayment.setId("PAY-123456789");
        expectedPayment.setState("created");

        when(payPalClient.createOrder(amount, currency, orderNumber)).thenReturn(expectedPayment);

        // When
        Payment result = payPalService.createOrder(amount, currency, orderNumber);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("PAY-123456789");
        assertThat(result.getState()).isEqualTo("created");
    }

    @Test
    void shouldCreateOrderWithZeroAmount() throws Exception {
        // Given
        double amount = 0.0;
        String currency = "USD";
        String orderNumber = "ORD-ZERO";

        Payment expectedPayment = new Payment();
        expectedPayment.setId("PAY-ZERO-123");
        expectedPayment.setState("created");

        when(payPalClient.createOrder(amount, currency, orderNumber)).thenReturn(expectedPayment);

        // When
        Payment result = payPalService.createOrder(amount, currency, orderNumber);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("PAY-ZERO-123");
        assertThat(result.getState()).isEqualTo("created");
    }

    @Test
    void shouldCreateOrderWithLargeAmount() throws Exception {
        // Given
        double amount = 9999.99;
        String currency = "USD";
        String orderNumber = "ORD-LARGE";

        Payment expectedPayment = new Payment();
        expectedPayment.setId("PAY-LARGE-123");
        expectedPayment.setState("created");

        when(payPalClient.createOrder(amount, currency, orderNumber)).thenReturn(expectedPayment);

        // When
        Payment result = payPalService.createOrder(amount, currency, orderNumber);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("PAY-LARGE-123");
        assertThat(result.getState()).isEqualTo("created");
    }

    @Test
    void shouldCreateOrderWithDifferentCurrencies() throws Exception {
        // Given
        double amount = 100.0;
        String currency = "EUR";
        String orderNumber = "ORD-EUR";

        Payment expectedPayment = new Payment();
        expectedPayment.setId("PAY-EUR-123");
        expectedPayment.setState("created");

        when(payPalClient.createOrder(amount, currency, orderNumber)).thenReturn(expectedPayment);

        // When
        Payment result = payPalService.createOrder(amount, currency, orderNumber);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("PAY-EUR-123");
        assertThat(result.getState()).isEqualTo("created");
    }

    @Test
    void shouldCaptureOrderSuccessfully() throws Exception {
        // Given
        String paymentId = "PAY-123456789";
        String payerId = "PAYER-123";

        Payment expectedPayment = new Payment();
        expectedPayment.setId(paymentId);
        expectedPayment.setState("approved");

        when(payPalClient.captureOrder(paymentId, payerId)).thenReturn(expectedPayment);

        // When
        Payment result = payPalService.captureOrder(paymentId, payerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(paymentId);
        assertThat(result.getState()).isEqualTo("approved");
    }

    @Test
    void shouldHandleOrderCreationFailure() throws Exception {
        // Given
        when(payPalClient.createOrder(anyDouble(), anyString(), anyString()))
                .thenThrow(new PayPalRESTException("Failed to create order"));

        // When & Then
        assertThatThrownBy(() -> payPalService.createOrder(100.0, "USD", "ORD-123"))
                .isInstanceOf(PayPalRESTException.class)
                .hasMessageContaining("Failed to create order");
    }

    @Test
    void shouldHandlePaymentCaptureFailure() throws Exception {
        // Given
        when(payPalClient.captureOrder(anyString(), anyString()))
                .thenThrow(new PayPalRESTException("Failed to capture payment"));

        // When & Then
        assertThatThrownBy(() -> payPalService.captureOrder("PAY-123", "PAYER-123"))
                .isInstanceOf(PayPalRESTException.class)
                .hasMessageContaining("Failed to capture payment");
    }
} 
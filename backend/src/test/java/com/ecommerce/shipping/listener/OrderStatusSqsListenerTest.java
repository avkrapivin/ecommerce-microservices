package com.ecommerce.shipping.listener;

import com.ecommerce.shipping.event.OrderStatusUpdateListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderStatusSqsListenerTest {

    @Mock
    private OrderStatusUpdateListener orderStatusUpdateListener;

    @Mock
    private SqsClient sqsClient;

    private OrderStatusSqsListener sqsListener;

    private final String queueUrl = "https://sqs.us-east-1.amazonaws.com/123456789/OrderStatusUpdateQueue";

    @BeforeEach
    void setUp() {
        sqsListener = new OrderStatusSqsListener(
                orderStatusUpdateListener, 
                sqsClient, 
                queueUrl
        );
    }

    @Test
    void shouldProcessSqsMessage() {
        // Given
        String messageBody = "{\"orderId\":\"123\",\"status\":\"SHIPPED\",\"trackingNumber\":\"TRACK123\"}";
        Message message = Message.builder()
                .messageId("msg-123")
                .body(messageBody)
                .receiptHandle("receipt-123")
                .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(message)
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(response);

        // When
        sqsListener.pollMessages();

        // Then
        verify(orderStatusUpdateListener).handleOrderStatusUpdate(messageBody);
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldHandleEmptyQueue() {
        // Given
        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(Collections.emptyList())
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(response);

        // When
        sqsListener.pollMessages();

        // Then
        verify(orderStatusUpdateListener, never()).handleOrderStatusUpdate(anyString());
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void shouldNotDeleteMessageOnProcessingError() {
        // Given
        String messageBody = "{\"orderId\":\"123\",\"status\":\"INVALID\"}";
        Message message = Message.builder()
                .messageId("msg-123")
                .body(messageBody)
                .receiptHandle("receipt-123")
                .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(message)
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(response);
        
        doThrow(new RuntimeException("Processing error"))
                .when(orderStatusUpdateListener).handleOrderStatusUpdate(messageBody);

        // When
        sqsListener.pollMessages();

        // Then
        verify(orderStatusUpdateListener).handleOrderStatusUpdate(messageBody);
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }
} 
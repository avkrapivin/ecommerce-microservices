package com.ecommerce.common.service;

import com.ecommerce.common.exception.ExceptionEvent;
import com.ecommerce.common.exception.ExceptionLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExceptionPublisherTest {

    @Mock
    private SnsClient snsClient;

    @Mock
    private ObjectMapper objectMapper;

    private ExceptionPublisher publisher;
    private final String topicArn = "arn:aws:sns:us-east-1:123456789012:Exception";
    private final String serviceName = "test-service";
    private final String environment = "test";

    @BeforeEach
    void setUp() {
        publisher = new ExceptionPublisher(snsClient, objectMapper, topicArn, serviceName, environment);
    }

    @Test
    void shouldPublishExceptionSuccessfully() throws JsonProcessingException {
        // Given
        Exception exception = new NullPointerException("Test exception");
        String jsonMessage = "{\"service\":\"test-service\"}";
        
        when(objectMapper.writeValueAsString(any(ExceptionEvent.class))).thenReturn(jsonMessage);
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("msg-123").build());

        // When
        publisher.publishException(exception);

        // Then
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        
        PublishRequest request = captor.getValue();
        assertThat(request.topicArn()).isEqualTo(topicArn);
        assertThat(request.message()).isEqualTo(jsonMessage);
        assertThat(request.subject()).contains("NullPointerException");
        assertThat(request.subject()).contains(serviceName);
    }

    @Test
    void shouldPublishExceptionWithContext() throws JsonProcessingException {
        // Given
        Exception exception = new IllegalArgumentException("Invalid argument");
        Map<String, Object> context = new HashMap<>();
        context.put("userId", "123");
        context.put("requestId", "req-456");
        
        String jsonMessage = "{\"service\":\"test-service\",\"context\":{\"userId\":\"123\"}}";
        
        when(objectMapper.writeValueAsString(any(ExceptionEvent.class))).thenReturn(jsonMessage);
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("msg-124").build());

        // When
        publisher.publishException(exception, context);

        // Then
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        
        PublishRequest request = captor.getValue();
        assertThat(request.message()).isEqualTo(jsonMessage);
        verify(objectMapper).writeValueAsString(any(ExceptionEvent.class));
    }

    @Test
    void shouldPublishExceptionWithLevelAndTags() throws JsonProcessingException {
        // Given
        Exception exception = new RuntimeException("Runtime error");
        String jsonMessage = "{\"service\":\"test-service\",\"level\":\"FATAL\"}";
        
        when(objectMapper.writeValueAsString(any(ExceptionEvent.class))).thenReturn(jsonMessage);
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("msg-125").build());

        // When
        publisher.publishException(exception, ExceptionLevel.FATAL, "critical", "payment");

        // Then
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        
        PublishRequest request = captor.getValue();
        assertThat(request.message()).isEqualTo(jsonMessage);
        assertThat(request.subject()).contains("RuntimeException");
    }

    @Test
    void shouldPublishExceptionWithFullControl() throws JsonProcessingException {
        // Given
        Exception exception = new SecurityException("Access denied");
        Map<String, Object> context = Map.of("endpoint", "/admin", "method", "POST");
        String jsonMessage = "{\"service\":\"test-service\",\"level\":\"ERROR\"}";
        
        when(objectMapper.writeValueAsString(any(ExceptionEvent.class))).thenReturn(jsonMessage);
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("msg-126").build());

        // When
        publisher.publishException(exception, ExceptionLevel.ERROR, context, "security", "admin");

        // Then
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        
        PublishRequest request = captor.getValue();
        assertThat(request.message()).isEqualTo(jsonMessage);
        verify(objectMapper).writeValueAsString(any(ExceptionEvent.class));
    }

    @Test
    void shouldPublishCustomError() throws JsonProcessingException {
        // Given
        String errorType = "ValidationError";
        String message = "Invalid input data";
        String jsonMessage = "{\"service\":\"test-service\",\"exceptionType\":\"ValidationError\"}";
        
        when(objectMapper.writeValueAsString(any(ExceptionEvent.class))).thenReturn(jsonMessage);
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("msg-127").build());

        // When
        publisher.publishCustomError(errorType, message, ExceptionLevel.WARN);

        // Then
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        
        PublishRequest request = captor.getValue();
        assertThat(request.message()).isEqualTo(jsonMessage);
        assertThat(request.subject()).contains("ValidationError");
        assertThat(request.subject()).contains(serviceName);
    }

    @Test
    void shouldHandleJsonProcessingExceptionGracefully() throws JsonProcessingException {
        // Given
        Exception exception = new RuntimeException("Test exception");
        when(objectMapper.writeValueAsString(any(ExceptionEvent.class)))
                .thenThrow(new JsonProcessingException("JSON error") {});

        // When
        publisher.publishException(exception);

        // Then
        verify(snsClient, never()).publish(any(PublishRequest.class));
        verify(objectMapper).writeValueAsString(any(ExceptionEvent.class));
    }

    @Test
    void shouldHandleSnsExceptionGracefully() throws JsonProcessingException {
        // Given
        Exception exception = new RuntimeException("Test exception");
        String jsonMessage = "{\"service\":\"test-service\"}";
        
        when(objectMapper.writeValueAsString(any(ExceptionEvent.class))).thenReturn(jsonMessage);
        when(snsClient.publish(any(PublishRequest.class)))
                .thenThrow(SnsException.builder().message("SNS error").build());

        // When
        publisher.publishException(exception);

        // Then
        verify(snsClient).publish(any(PublishRequest.class));
        verify(objectMapper).writeValueAsString(any(ExceptionEvent.class));
    }

    @Test
    void shouldReturnTrueWhenEnabled() {
        // When & Then
        assertThat(publisher.isEnabled()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenTopicArnIsNull() {
        // Given
        ExceptionPublisher disabledPublisher = new ExceptionPublisher(
                snsClient, objectMapper, null, serviceName, environment);

        // When & Then
        assertThat(disabledPublisher.isEnabled()).isFalse();
    }

    @Test
    void shouldReturnFalseWhenTopicArnIsEmpty() {
        // Given
        ExceptionPublisher disabledPublisher = new ExceptionPublisher(
                snsClient, objectMapper, "", serviceName, environment);

        // When & Then
        assertThat(disabledPublisher.isEnabled()).isFalse();
    }

    @Test
    void shouldReturnFalseWhenTopicArnIsBlank() {
        // Given
        ExceptionPublisher disabledPublisher = new ExceptionPublisher(
                snsClient, objectMapper, "   ", serviceName, environment);

        // When & Then
        assertThat(disabledPublisher.isEnabled()).isFalse();
    }

    @Test
    void shouldCreateCorrectSubjectForException() throws JsonProcessingException {
        // Given
        Exception exception = new IllegalStateException("Invalid state");
        when(objectMapper.writeValueAsString(any(ExceptionEvent.class))).thenReturn("{}");
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("msg-128").build());

        // When
        publisher.publishException(exception);

        // Then
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        
        PublishRequest request = captor.getValue();
        assertThat(request.subject()).isEqualTo("Exception: IllegalStateException in test-service");
    }

    @Test
    void shouldCreateCorrectSubjectForCustomError() throws JsonProcessingException {
        // Given
        String errorType = "BusinessLogicError";
        when(objectMapper.writeValueAsString(any(ExceptionEvent.class))).thenReturn("{}");
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("msg-129").build());

        // When
        publisher.publishCustomError(errorType, "Error message", ExceptionLevel.ERROR);

        // Then
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        
        PublishRequest request = captor.getValue();
        assertThat(request.subject()).isEqualTo("Exception: BusinessLogicError in test-service");
    }

    @Test
    void shouldVerifyExceptionEventCreation() throws JsonProcessingException {
        // Given
        Exception exception = new NullPointerException("Null pointer");
        when(objectMapper.writeValueAsString(any(ExceptionEvent.class))).thenReturn("{}");
        when(snsClient.publish(any(PublishRequest.class)))
                .thenReturn(PublishResponse.builder().messageId("msg-130").build());

        // When
        publisher.publishException(exception);

        // Then
        ArgumentCaptor<ExceptionEvent> eventCaptor = ArgumentCaptor.forClass(ExceptionEvent.class);
        verify(objectMapper).writeValueAsString(eventCaptor.capture());
        
        ExceptionEvent event = eventCaptor.getValue();
        assertThat(event.getService()).isEqualTo(serviceName);
        assertThat(event.getEnvironment()).isEqualTo(environment);
        assertThat(event.getExceptionType()).isEqualTo("NullPointerException");
        assertThat(event.getMessage()).isEqualTo("Null pointer");
        assertThat(event.getTimestamp()).isNotNull();
    }
} 
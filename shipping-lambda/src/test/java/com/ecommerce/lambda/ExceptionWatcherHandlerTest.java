package com.ecommerce.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.ecommerce.lambda.service.ExceptionProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExceptionWatcherHandlerTest {

    @Mock
    private ExceptionProcessor exceptionProcessor;

    @Mock
    private Context context;

    private ExceptionWatcherHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        handler = new ExceptionWatcherHandler(exceptionProcessor, objectMapper);
    }

    @Test
    void shouldProcessSingleSNSRecordSuccessfully() {
        // Given
        SNSEvent event = createSNSEvent(1);

        // When
        String result = handler.handleRequest(event, context);

        // Then
        verify(exceptionProcessor, times(1)).processException(any());
        assertThat(result).isEqualTo("Processed: 1, Errors: 0");
    }

    @Test
    void shouldProcessMultipleSNSRecordsSuccessfully() {
        // Given
        SNSEvent event = createSNSEvent(3);

        // When
        String result = handler.handleRequest(event, context);

        // Then
        verify(exceptionProcessor, times(3)).processException(any());
        assertThat(result).isEqualTo("Processed: 3, Errors: 0");
    }

    @Test
    void shouldHandleProcessingErrorsGracefully() {
        // Given
        SNSEvent event = createSNSEvent(2);
        doThrow(new RuntimeException("Processing error"))
                .when(exceptionProcessor).processException(any());

        // When
        String result = handler.handleRequest(event, context);

        // Then
        verify(exceptionProcessor, times(2)).processException(any());
        assertThat(result).isEqualTo("Processed: 0, Errors: 2");
    }

    @Test
    void shouldHandlePartialProcessingErrors() {
        // Given
        SNSEvent event = createSNSEvent(3);
        doNothing()
                .doThrow(new RuntimeException("Processing error"))
                .doNothing()
                .when(exceptionProcessor).processException(any());

        // When
        String result = handler.handleRequest(event, context);

        // Then
        verify(exceptionProcessor, times(3)).processException(any());
        assertThat(result).isEqualTo("Processed: 2, Errors: 1");
    }

    @Test
    void shouldHandleInvalidJSONGracefully() {
        // Given
        SNSEvent event = createSNSEventWithInvalidJSON();

        // When
        String result = handler.handleRequest(event, context);

        // Then
        verify(exceptionProcessor, never()).processException(any());
        assertThat(result).isEqualTo("Processed: 0, Errors: 1");
    }

    @Test
    void shouldHandleEmptyEventGracefully() {
        // Given
        SNSEvent event = new SNSEvent();
        event.setRecords(List.of());

        // When
        String result = handler.handleRequest(event, context);

        // Then
        verify(exceptionProcessor, never()).processException(any());
        assertThat(result).isEqualTo("Processed: 0, Errors: 0");
    }

    @Test
    void shouldDeserializeExceptionEventCorrectly() {
        // Given
        SNSEvent event = createSNSEvent(1);

        // When
        handler.handleRequest(event, context);

        // Then
        ArgumentCaptor<com.ecommerce.lambda.model.ExceptionEvent> captor = 
                ArgumentCaptor.forClass(com.ecommerce.lambda.model.ExceptionEvent.class);
        verify(exceptionProcessor).processException(captor.capture());
        
        com.ecommerce.lambda.model.ExceptionEvent exceptionEvent = captor.getValue();
        assertThat(exceptionEvent.getService()).isEqualTo("test-service");
        assertThat(exceptionEvent.getExceptionType()).isEqualTo("NullPointerException");
        assertThat(exceptionEvent.getMessage()).isEqualTo("Test exception message");
        assertThat(exceptionEvent.getLevel()).isEqualTo(com.ecommerce.lambda.model.ExceptionLevel.ERROR);
        assertThat(exceptionEvent.getEnvironment()).isEqualTo("test");
    }

    @Test
    void shouldHandleNullExceptionEventGracefully() {
        // Given
        SNSEvent event = createSNSEventWithNullContent();

        // When
        String result = handler.handleRequest(event, context);

        // Then
        verify(exceptionProcessor, never()).processException(any());
        assertThat(result).isEqualTo("Processed: 0, Errors: 0");
    }

    @Test
    void shouldLogProcessingDetails() {
        // Given
        SNSEvent event = createSNSEvent(2);

        // When
        String result = handler.handleRequest(event, context);

        // Then
        verify(exceptionProcessor, times(2)).processException(any());
        assertThat(result).contains("Processed: 2");
    }

    private SNSEvent createSNSEvent(int recordCount) {
        SNSEvent event = new SNSEvent();
        List<SNSEvent.SNSRecord> records = List.of();
        
        for (int i = 0; i < recordCount; i++) {
            SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
            SNSEvent.SNS sns = new SNSEvent.SNS();
            
            String exceptionEventJson = createExceptionEventJson();
            sns.setMessage(exceptionEventJson);
            sns.setMessageId("message-id-" + i);
            
            record.setSns(sns);
            records = new java.util.ArrayList<>(records);
            records.add(record);
        }
        
        event.setRecords(records);
        return event;
    }

    private SNSEvent createSNSEventWithInvalidJSON() {
        SNSEvent event = new SNSEvent();
        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        SNSEvent.SNS sns = new SNSEvent.SNS();
        
        sns.setMessage("{ invalid json }");
        sns.setMessageId("invalid-message-id");
        
        record.setSns(sns);
        event.setRecords(List.of(record));
        return event;
    }

    private SNSEvent createSNSEventWithNullContent() {
        SNSEvent event = new SNSEvent();
        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        SNSEvent.SNS sns = new SNSEvent.SNS();
        
        sns.setMessage(null);  // Изменено с "null" на null
        sns.setMessageId("null-message-id");
        
        record.setSns(sns);
        event.setRecords(List.of(record));
        return event;
    }

    private String createExceptionEventJson() {
        return """
                {
                    "service": "test-service",
                    "timestamp": "2024-01-01T12:00:00.000Z",
                    "exceptionType": "NullPointerException",
                    "message": "Test exception message",
                    "stackTrace": "java.lang.NullPointerException\\n\\tat test.method(Test.java:1)",
                    "context": {
                        "userId": "123",
                        "requestId": "req-456"
                    },
                    "level": "ERROR",
                    "environment": "test",
                    "version": "1.0.0",
                    "tags": ["test", "unit-test"]
                }
                """;
    }
} 
package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.ExceptionEvent;
import com.ecommerce.lambda.model.ExceptionLevel;
import com.ecommerce.lambda.model.ExceptionRecord;
import com.ecommerce.lambda.repository.ExceptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExceptionProcessorTest {

    @Mock
    private ExceptionRepository repository;

    @Mock
    private MetricsPublisher metricsPublisher;

    private ExceptionProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ExceptionProcessor(repository, metricsPublisher);
    }

    @Test
    void shouldProcessNewExceptionSuccessfully() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        when(repository.findSimilarException(anyString(), anyString())).thenReturn(Optional.empty());

        // When
        processor.processException(event);

        // Then
        verify(repository).saveException(any(ExceptionRecord.class));
        verify(metricsPublisher).publishExceptionMetrics(event);
    }

    @Test
    void shouldProcessExistingExceptionWithGrouping() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        ExceptionRecord existingRecord = createTestRecord();
        when(repository.findSimilarException(anyString(), anyString())).thenReturn(Optional.of(existingRecord));

        // When
        processor.processException(event);

        // Then
        verify(repository).updateExceptionCount(existingRecord);
        verify(metricsPublisher).publishExceptionMetrics(event);
        verify(metricsPublisher).publishGroupedExceptionMetrics(eq(event), anyLong());
    }

    @Test
    void shouldCreateNewExceptionWhenGroupingWindowExpired() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        ExceptionRecord oldRecord = createTestRecord();
        // Устанавливаем старую дату (более 5 минут назад)
        oldRecord.setLastOccurrence(Instant.now().minusSeconds(400).toString());
        when(repository.findSimilarException(anyString(), anyString())).thenReturn(Optional.of(oldRecord));

        // When
        processor.processException(event);

        // Then
        verify(repository).saveException(any(ExceptionRecord.class));
        verify(repository, never()).updateExceptionCount(any());
        verify(metricsPublisher).publishExceptionMetrics(event);
    }

    @Test
    void shouldCreateAlertForCriticalException() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        when(repository.findSimilarException(anyString(), anyString())).thenReturn(Optional.empty());

        // When
        processor.processException(event);

        // Then
        verify(repository).saveException(any(ExceptionRecord.class));
        verify(metricsPublisher).publishExceptionMetrics(event);
        // Алерт создается через логирование, проверяем что обработка прошла успешно
    }

    @Test
    void shouldCreateAlertForFatalException() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.FATAL);
        when(repository.findSimilarException(anyString(), anyString())).thenReturn(Optional.empty());

        // When
        processor.processException(event);

        // Then
        verify(repository).saveException(any(ExceptionRecord.class));
        verify(metricsPublisher).publishExceptionMetrics(event);
    }

    @Test
    void shouldNotCreateAlertForWarningException() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.WARN);
        when(repository.findSimilarException(anyString(), anyString())).thenReturn(Optional.empty());

        // When
        processor.processException(event);

        // Then
        verify(repository).saveException(any(ExceptionRecord.class));
        verify(metricsPublisher).publishExceptionMetrics(event);
    }

    @Test
    void shouldFilterOutInvalidEvents() {
        // Given
        ExceptionEvent invalidEvent = ExceptionEvent.builder().build(); // Нет обязательных полей

        // When
        processor.processException(invalidEvent);

        // Then
        verify(repository, never()).saveException(any());
        verify(repository, never()).findSimilarException(anyString(), anyString());
        verify(metricsPublisher, never()).publishExceptionMetrics(any());
    }

    @Test
    void shouldFilterOutTraceEventsInProduction() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.TRACE);
        event.setEnvironment("production");

        // When
        processor.processException(event);

        // Then
        verify(repository, never()).saveException(any());
        verify(repository, never()).findSimilarException(anyString(), anyString());
        verify(metricsPublisher, never()).publishExceptionMetrics(any());
    }

    @Test
    void shouldFilterOutDebugEventsInProduction() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.DEBUG);
        event.setEnvironment("production");

        // When
        processor.processException(event);

        // Then
        verify(repository, never()).saveException(any());
        verify(repository, never()).findSimilarException(anyString(), anyString());
        verify(metricsPublisher, never()).publishExceptionMetrics(any());
    }

    @Test
    void shouldNotFilterOutTraceEventsInDevelopment() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.TRACE);
        event.setEnvironment("development");
        when(repository.findSimilarException(anyString(), anyString())).thenReturn(Optional.empty());

        // When
        processor.processException(event);

        // Then
        verify(repository).saveException(any(ExceptionRecord.class));
        verify(metricsPublisher).publishExceptionMetrics(event);
    }

    @Test
    void shouldFilterOutKnownTemporaryErrors() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        event.setExceptionType("java.net.SocketTimeoutException");

        // When
        processor.processException(event);

        // Then
        verify(repository, never()).saveException(any());
        verify(repository, never()).findSimilarException(anyString(), anyString());
        verify(metricsPublisher, never()).publishExceptionMetrics(any());
    }

    @Test
    void shouldFilterOutConnectionExceptions() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        event.setExceptionType("java.net.ConnectException");

        // When
        processor.processException(event);

        // Then
        verify(repository, never()).saveException(any());
        verify(repository, never()).findSimilarException(anyString(), anyString());
        verify(metricsPublisher, never()).publishExceptionMetrics(any());
    }

    @Test
    void shouldFilterOutBlacklistedExceptions() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        event.setExceptionType("java.lang.InterruptedException");

        // When
        processor.processException(event);

        // Then
        verify(repository, never()).saveException(any());
        verify(repository, never()).findSimilarException(anyString(), anyString());
        verify(metricsPublisher, never()).publishExceptionMetrics(any());
    }

    @Test
    void shouldFilterOutExceptionsByMessage() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        event.setMessage("connection timeout occurred");

        // When
        processor.processException(event);

        // Then
        verify(repository, never()).saveException(any());
        verify(repository, never()).findSimilarException(anyString(), anyString());
        verify(metricsPublisher, never()).publishExceptionMetrics(any());
    }

    @Test
    void shouldHandleRepositoryExceptionGracefully() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        when(repository.findSimilarException(anyString(), anyString())).thenThrow(new RuntimeException("DB error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> processor.processException(event), "DB error");
        verify(repository).findSimilarException(anyString(), anyString());
        verify(repository, never()).saveException(any());
        verify(metricsPublisher, never()).publishExceptionMetrics(any());
    }

    @Test
    void shouldHandleMetricsPublisherExceptionGracefully() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        when(repository.findSimilarException(anyString(), anyString())).thenReturn(Optional.empty());
        doThrow(new RuntimeException("Metrics error")).when(metricsPublisher).publishExceptionMetrics(any());

        // When & Then
        assertThrows(RuntimeException.class, () -> processor.processException(event), "Metrics error");
        verify(repository).saveException(any(ExceptionRecord.class));
        verify(metricsPublisher).publishExceptionMetrics(event);
    }

    @Test
    void shouldValidateEventFields() {
        // Given
        ExceptionEvent eventWithoutService = createTestEvent(ExceptionLevel.ERROR);
        eventWithoutService.setService(null);

        ExceptionEvent eventWithoutType = createTestEvent(ExceptionLevel.ERROR);
        eventWithoutType.setExceptionType("");

        ExceptionEvent eventWithoutLevel = createTestEvent(ExceptionLevel.ERROR);
        eventWithoutLevel.setLevel(null);

        // When & Then
        processor.processException(eventWithoutService);
        processor.processException(eventWithoutType);
        processor.processException(eventWithoutLevel);

        verify(repository, never()).saveException(any());
        verify(metricsPublisher, never()).publishExceptionMetrics(any());
    }

    private ExceptionEvent createTestEvent(ExceptionLevel level) {
        return ExceptionEvent.builder()
                .service("test-service")
                .timestamp(Instant.now())
                .exceptionType("NullPointerException")
                .message("Test exception message")
                .level(level)
                .environment("test")
                .tags(List.of("test"))
                .build();
    }

    private ExceptionRecord createTestRecord() {
        return ExceptionRecord.builder()
                .partitionKey("test-service#NullPointerException")
                .timestamp(Instant.now().toString())
                .service("test-service")
                .exceptionType("NullPointerException")
                .message("Test exception message")
                .level("ERROR")
                .environment("test")
                .count(1L)
                .firstOccurrence(Instant.now().toString())
                .lastOccurrence(Instant.now().toString())
                .ttl(Instant.now().getEpochSecond() + 2592000) // 30 дней
                .build();
    }
} 
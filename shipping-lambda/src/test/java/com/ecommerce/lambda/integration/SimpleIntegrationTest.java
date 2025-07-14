package com.ecommerce.lambda.integration;

import com.ecommerce.lambda.model.ExceptionEvent;
import com.ecommerce.lambda.model.ExceptionLevel;
import com.ecommerce.lambda.service.ExceptionProcessor;
import com.ecommerce.lambda.service.MetricsPublisher;
import com.ecommerce.lambda.repository.ExceptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Упрощенный интеграционный тест для проверки основной логики
 */
class SimpleIntegrationTest {

    @Mock
    private DynamoDbClient dynamoDbClient;
    
    @Mock
    private CloudWatchClient cloudWatchClient;
    
    private ExceptionRepository exceptionRepository;
    private MetricsPublisher metricsPublisher;
    private ExceptionProcessor exceptionProcessor;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        exceptionRepository = new ExceptionRepository(dynamoDbClient, "test-table");
        metricsPublisher = new MetricsPublisher(cloudWatchClient, "ECommerce/Test/Exceptions");
        exceptionProcessor = new ExceptionProcessor(exceptionRepository, metricsPublisher);
    }

    @Test
    void shouldProcessExceptionSuccessfully() {
        // Given
        ExceptionEvent event = ExceptionEvent.builder()
                .service("test-service")
                .exceptionType("RuntimeException")
                .message("Test exception message")
                .level(ExceptionLevel.ERROR)
                .environment("test")
                .timestamp(Instant.now())
                .correlationId("test-correlation-123")
                .userId("test-user-456")
                .stackTrace("java.lang.RuntimeException: Test exception")
                .context(Map.of("requestId", "req-123"))
                .tags(List.of("web-request"))
                .build();
        
        // When
        exceptionProcessor.processException(event);
        
        // Then - проверяем, что методы были вызваны
        verify(dynamoDbClient, atLeastOnce()).putItem(ArgumentMatchers.<PutItemRequest>any());
        verify(cloudWatchClient, atLeastOnce()).putMetricData(ArgumentMatchers.<PutMetricDataRequest>any());
    }

    @Test
    void shouldHandleFatalExceptions() {
        // Given
        ExceptionEvent event = ExceptionEvent.builder()
                .service("test-service")
                .exceptionType("RuntimeException")
                .message("Fatal error occurred")
                .level(ExceptionLevel.FATAL)
                .environment("prod")
                .timestamp(Instant.now())
                .correlationId("test-correlation-456")
                .userId("test-user-789")
                .stackTrace("java.lang.RuntimeException: Fatal error")
                .context(Map.of("requestId", "req-456"))
                .tags(List.of("critical"))
                .build();
        
        // When
        exceptionProcessor.processException(event);
        
        // Then - проверяем, что создались метрики для критических ошибок
        verify(cloudWatchClient, atLeastOnce()).putMetricData(ArgumentMatchers.<PutMetricDataRequest>any());
        verify(dynamoDbClient, atLeastOnce()).putItem(ArgumentMatchers.<PutItemRequest>any());
    }

    @Test
    void shouldFilterOutTraceInProduction() {
        // Given
        ExceptionEvent event = ExceptionEvent.builder()
                .service("test-service")
                .exceptionType("RuntimeException")
                .message("Trace level message")
                .level(ExceptionLevel.TRACE)
                .environment("production")  // Изменено с "prod" на "production"
                .timestamp(Instant.now())
                .correlationId("test-correlation-789")
                .userId("test-user-999")
                .stackTrace("java.lang.RuntimeException: Trace")
                .context(Map.of("requestId", "req-789"))
                .tags(List.of("trace"))
                .build();
        
        // When
        exceptionProcessor.processException(event);
        
        // Then - TRACE события не должны обрабатываться в production
        verify(dynamoDbClient, never()).putItem(ArgumentMatchers.<PutItemRequest>any());
        verify(cloudWatchClient, never()).putMetricData(ArgumentMatchers.<PutMetricDataRequest>any());
    }
} 
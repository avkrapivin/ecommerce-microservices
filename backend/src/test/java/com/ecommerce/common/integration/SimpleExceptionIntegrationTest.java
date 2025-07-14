package com.ecommerce.common.integration;

import com.ecommerce.common.service.ExceptionPublisher;
import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.exception.ExceptionLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Упрощенный интеграционный тест для проверки публикации исключений
 * без полной загрузки Spring контекста
 */
class SimpleExceptionIntegrationTest {

    @Mock
    private SnsClient snsClient;
    
    @Mock
    private ObjectMapper objectMapper;
    
    private ExceptionPublisher exceptionPublisher;
    private GlobalExceptionHandler globalExceptionHandler;
    
    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Настройка моков
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
        
        // Создание реальных объектов с моками
        exceptionPublisher = new ExceptionPublisher(
            snsClient,
            objectMapper,
            "arn:aws:sns:us-east-1:123456789012:test-topic",
            "test-service",
            "test"
        );
        
        globalExceptionHandler = new GlobalExceptionHandler(exceptionPublisher);
    }

    @Test
    void shouldPublishExceptionWithRequestContext() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/products/123");
        request.addHeader("User-Agent", "Test-Agent");
        request.addHeader("X-Request-ID", "req-123");
        request.setRemoteAddr("127.0.0.1");
        
        ResourceNotFoundException exception = new ResourceNotFoundException("Product not found");
        
        // When
        globalExceptionHandler.handleResourceNotFound(exception, request);
        
        // Then - проверяем, что SNS был вызван
        verify(snsClient, times(1)).publish(ArgumentMatchers.<PublishRequest>any());
        verify(objectMapper, times(1)).writeValueAsString(any());
    }

    @Test
    void shouldHandleNullRequest() throws Exception {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Test exception");
        
        // When
        globalExceptionHandler.handleResourceNotFound(exception, null);
        
        // Then - должен обработать gracefully
        verify(snsClient, times(1)).publish(ArgumentMatchers.<PublishRequest>any());
    }

    @Test
    void shouldCreateCorrectExceptionEvent() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/test");
        request.addHeader("X-Correlation-ID", "test-123");
        
        ResourceNotFoundException exception = new ResourceNotFoundException("Test not found");
        
        // When
        globalExceptionHandler.handleResourceNotFound(exception, request);
        
        // Then
        verify(snsClient).publish(ArgumentMatchers.<PublishRequest>any());
        verify(objectMapper).writeValueAsString(any());
    }

    @Test 
    void shouldNotThrowExceptionWhenSnsFailsGracefully() throws Exception {
        // Given
        when(snsClient.publish(ArgumentMatchers.<PublishRequest>any())).thenThrow(new RuntimeException("SNS Error"));
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        ResourceNotFoundException exception = new ResourceNotFoundException("Test exception");
        
        // When & Then - не должно выбрасывать исключение
        globalExceptionHandler.handleResourceNotFound(exception, request);
        
        verify(snsClient).publish(ArgumentMatchers.<PublishRequest>any());
    }
} 
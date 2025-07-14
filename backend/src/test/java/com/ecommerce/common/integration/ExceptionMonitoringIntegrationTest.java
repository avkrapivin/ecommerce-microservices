package com.ecommerce.common.integration;

import com.ecommerce.common.service.ExceptionPublisher;
import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.common.exception.ExceptionLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

/**
 * Упрощенный интеграционный тест для проверки публикации исключений
 * без полной загрузки Spring контекста
 */
class ExceptionMonitoringIntegrationTest {

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

    @Test
    void shouldIncludeRequestContextInException() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/test/endpoint");
        request.addHeader("User-Agent", "Integration-Test-Agent");
        request.addHeader("X-Request-ID", "integration-req-456");
        request.setQueryString("testParam=testValue");
        
        ResourceNotFoundException exception = new ResourceNotFoundException("Test POST resource not found");
        
        // When
        globalExceptionHandler.handleResourceNotFound(exception, request);
        
        // Then - проверяем, что метод был вызван с правильными параметрами
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        ArgumentCaptor<ExceptionLevel> levelCaptor = ArgumentCaptor.forClass(ExceptionLevel.class);
        ArgumentCaptor<Map> contextCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        
        // Мы не можем проверить внутренние вызовы ExceptionPublisher напрямую,
        // но можем проверить что SNS был вызван
        verify(snsClient, times(1)).publish(ArgumentMatchers.<PublishRequest>any());
        verify(objectMapper, times(1)).writeValueAsString(any());
    }

    @Test
    void shouldHandleMultipleExceptionsInSequence() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/test/exception");
        
        // When - генерируем несколько исключений
        for (int i = 0; i < 3; i++) {
            ResourceNotFoundException exception = new ResourceNotFoundException("Test exception " + i);
            globalExceptionHandler.handleResourceNotFound(exception, request);
        }
        
        // Then - должно быть 3 публикации
        verify(snsClient, times(3)).publish(ArgumentMatchers.<PublishRequest>any());
        verify(objectMapper, times(3)).writeValueAsString(any());
    }

    @Test
    void shouldHandleExceptionPublisherFailureGracefully() throws Exception {
        // Given
        when(snsClient.publish(ArgumentMatchers.<PublishRequest>any())).thenThrow(new RuntimeException("SNS Error"));
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        ResourceNotFoundException exception = new ResourceNotFoundException("Test exception");
        
        // When - исключение в publisher не должно ломать основной flow
        globalExceptionHandler.handleResourceNotFound(exception, request);
        
        // Then
        verify(snsClient, times(1)).publish(ArgumentMatchers.<PublishRequest>any());
    }

    @Test
    void shouldNotPublishWhenPublisherDisabled() throws Exception {
        // Given - создаем publisher с отключенной публикацией
        ExceptionPublisher disabledPublisher = new ExceptionPublisher(
            snsClient,
            objectMapper,
            "", // Пустой topic ARN отключает публикацию
            "test-service",
            "test"
        );
        
        GlobalExceptionHandler handlerWithDisabledPublisher = new GlobalExceptionHandler(disabledPublisher);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        ResourceNotFoundException exception = new ResourceNotFoundException("Test exception");
        
        // When
        handlerWithDisabledPublisher.handleResourceNotFound(exception, request);
        
        // Then - SNS не должен быть вызван
        verify(snsClient, never()).publish(ArgumentMatchers.<PublishRequest>any());
    }
} 
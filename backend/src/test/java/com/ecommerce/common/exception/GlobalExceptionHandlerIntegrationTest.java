package com.ecommerce.common.exception;

import com.ecommerce.common.service.ExceptionPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerIntegrationTest {

    @Mock
    private ExceptionPublisher exceptionPublisher;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Principal principal;

    @Mock
    private BindingResult bindingResult;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(exceptionPublisher);
        
        // Настройка базового мока для request - только для тестов, которые его используют
        lenient().when(request.getMethod()).thenReturn("POST");
        lenient().when(request.getRequestURI()).thenReturn("/api/test");
        lenient().when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(request.getQueryString()).thenReturn("param=value");
        lenient().when(request.getHeader("X-Request-ID")).thenReturn("req-123");
        
        lenient().when(exceptionPublisher.isEnabled()).thenReturn(true);
    }

    @Test
    void shouldHandleResourceNotFoundExceptionAndPublishEvent() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Resource not found");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Resource not found");
        
        verify(exceptionPublisher).publishException(
                eq(exception), 
                eq(ExceptionLevel.WARN), 
                any(Map.class), 
                eq("web-request")
        );
    }

    @Test
    void shouldHandleOrderStatusExceptionAndPublishEvent() {
        // Given
        OrderStatusException exception = new OrderStatusException("Invalid order status");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleOrderStatusException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid order status");
        
        verify(exceptionPublisher).publishException(
                eq(exception), 
                eq(ExceptionLevel.ERROR), 
                any(Map.class), 
                eq("web-request")
        );
    }

    @Test
    void shouldHandleValidationExceptionAndPublishEvent() {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        FieldError fieldError = new FieldError("object", "field", "Validation error");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
        when(exception.getBindingResult()).thenReturn(bindingResult);

        // When
        ResponseEntity<Map<String, String>> response = handler.handleValidationExceptions(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("field", "Validation error");
        
        verify(exceptionPublisher).publishException(
                eq(exception), 
                eq(ExceptionLevel.WARN), 
                any(Map.class), 
                eq("web-request")
        );
    }

    @Test
    void shouldHandleIOExceptionAndPublishEvent() {
        // Given
        IOException exception = new IOException("IO error occurred");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleIOException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).contains("Failed to process shipping request");
        
        verify(exceptionPublisher).publishException(
                eq(exception), 
                eq(ExceptionLevel.ERROR), 
                any(Map.class), 
                eq("web-request")
        );
    }

    @Test
    void shouldHandleGeneralExceptionAndPublishEvent() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleGeneralException(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
        
        verify(exceptionPublisher).publishException(
                eq(exception), 
                eq(ExceptionLevel.FATAL), 
                any(Map.class), 
                eq("web-request")
        );
    }

    @Test
    void shouldIncludeRequestContextInPublishedEvent() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Test exception");

        // When
        handler.handleResourceNotFound(exception, request);

        // Then
        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
        verify(exceptionPublisher).publishException(
                any(), 
                any(), 
                contextCaptor.capture(), 
                any()
        );
        
        Map<String, Object> context = contextCaptor.getValue();
        assertThat(context).containsEntry("requestId", "req-123");
        assertThat(context).containsEntry("httpMethod", "POST");
        assertThat(context).containsEntry("endpoint", "/api/test");
        assertThat(context).containsEntry("userAgent", "Test-Agent");
        assertThat(context).containsEntry("remoteAddr", "127.0.0.1");
        assertThat(context).containsEntry("queryString", "param=value");
    }

    @Test
    void shouldIncludeUserIdWhenPrincipalExists() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Test exception");
        when(request.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("user123");

        // When
        handler.handleResourceNotFound(exception, request);

        // Then
        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
        verify(exceptionPublisher).publishException(
                any(), 
                any(), 
                contextCaptor.capture(), 
                any()
        );
        
        Map<String, Object> context = contextCaptor.getValue();
        assertThat(context).containsEntry("userId", "user123");
    }

    @Test
    void shouldHandleNullRequestGracefully() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Test exception");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(exception, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        
        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
        verify(exceptionPublisher).publishException(
                any(), 
                any(), 
                contextCaptor.capture(), 
                any()
        );
        
        Map<String, Object> context = contextCaptor.getValue();
        assertThat(context).isEmpty();
    }

    @Test
    void shouldNotPublishWhenPublisherIsDisabled() {
        // Given
        when(exceptionPublisher.isEnabled()).thenReturn(false);
        ResourceNotFoundException exception = new ResourceNotFoundException("Test exception");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(exceptionPublisher, never()).publishException(any(), any(), any(), any());
    }

    @Test
    void shouldHandlePublisherExceptionGracefully() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Test exception");
        doThrow(new RuntimeException("Publisher error"))
                .when(exceptionPublisher).publishException(any(), any(), any(), any());

        // When
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("Test exception");
        
        verify(exceptionPublisher).publishException(any(), any(), any(), any());
    }

    @Test
    void shouldHandleInsufficientStockExceptionAndPublishEvent() {
        // Given
        InsufficientStockException exception = new InsufficientStockException("Not enough stock");

        // When
        ResponseEntity<String> response = handler.handleInsufficientStock(exception, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Not enough stock");
        
        verify(exceptionPublisher).publishException(
                eq(exception), 
                eq(ExceptionLevel.ERROR), 
                any(Map.class), 
                eq("web-request")
        );
    }

    @Test
    void shouldCreateCorrectContextForDifferentRequestTypes() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Test exception");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/products/123");
        when(request.getQueryString()).thenReturn(null);

        // When
        handler.handleResourceNotFound(exception, request);

        // Then
        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
        verify(exceptionPublisher).publishException(
                any(), 
                any(), 
                contextCaptor.capture(), 
                any()
        );
        
        Map<String, Object> context = contextCaptor.getValue();
        assertThat(context).containsEntry("httpMethod", "GET");
        assertThat(context).containsEntry("endpoint", "/api/products/123");
        assertThat(context).containsEntry("queryString", null);
    }

    @Test
    void shouldVerifyAllExceptionHandlersPublishEvents() {
        // Given
        Exception[] exceptions = {
                new ResourceNotFoundException("Resource not found"),
                new OrderStatusException("Order status error"),
                new InsufficientStockException("Stock error"),
                new IOException("IO error"),
                new RuntimeException("Runtime error")
        };

        // When
        handler.handleResourceNotFound((ResourceNotFoundException) exceptions[0], request);
        handler.handleOrderStatusException((OrderStatusException) exceptions[1], request);
        handler.handleInsufficientStock((InsufficientStockException) exceptions[2], request);
        handler.handleIOException((IOException) exceptions[3], request);
        handler.handleGeneralException((RuntimeException) exceptions[4], request);

        // Then
        verify(exceptionPublisher, times(5)).publishException(any(), any(), any(), any());
    }
} 
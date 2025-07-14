package com.ecommerce.common.exception;

import com.ecommerce.common.service.ExceptionPublisher;
import com.ecommerce.shipping.exception.ShippingInfoNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final ExceptionPublisher exceptionPublisher;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        publishException(ex, request, ExceptionLevel.WARN);
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ShippingInfoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleShippingInfoNotFound(ShippingInfoNotFoundException ex, HttpServletRequest request) {
        publishException(ex, request, ExceptionLevel.WARN);
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OrderStatusException.class)
    public ResponseEntity<ErrorResponse> handleOrderStatusException(OrderStatusException ex, HttpServletRequest request) {
        publishException(ex, request, ExceptionLevel.ERROR);
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        publishException(ex, request, ExceptionLevel.WARN);
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        publishException(ex, request, ExceptionLevel.WARN);
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        }
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<String> handleInsufficientStock(InsufficientStockException ex, HttpServletRequest request) {
        publishException(ex, request, ExceptionLevel.ERROR);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex, HttpServletRequest request) {
        publishException(ex, request, ExceptionLevel.ERROR);
        ErrorResponse error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to process shipping request: " + ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        publishException(ex, request, ExceptionLevel.FATAL);
        log.error("Unhandled exception occurred", ex);
        ErrorResponse error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Публикует исключение в SNS с контекстной информацией
     */
    private void publishException(Exception ex, HttpServletRequest request, ExceptionLevel level) {
        try {
            if (exceptionPublisher.isEnabled()) {
                Map<String, Object> context = createRequestContext(request);
                exceptionPublisher.publishException(ex, level, context, "web-request");
            }
        } catch (Exception e) {
            log.error("Failed to publish exception to SNS", e);
            // Не прерываем обработку основного исключения
        }
    }

    /**
     * Создает контекст запроса для ошибки
     */
    private Map<String, Object> createRequestContext(HttpServletRequest request) {
        Map<String, Object> context = new HashMap<>();
        
        if (request != null) {
            context.put("requestId", request.getHeader("X-Request-ID"));
            context.put("httpMethod", request.getMethod());
            context.put("endpoint", request.getRequestURI());
            context.put("userAgent", request.getHeader("User-Agent"));
            context.put("remoteAddr", request.getRemoteAddr());
            context.put("queryString", request.getQueryString());
            
            // Добавляем информацию о пользователе, если доступна
            if (request.getUserPrincipal() != null) {
                context.put("userId", request.getUserPrincipal().getName());
            }
        }
        
        return context;
    }
}
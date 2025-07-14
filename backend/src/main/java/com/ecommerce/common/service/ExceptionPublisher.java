package com.ecommerce.common.service;

import com.ecommerce.common.exception.ExceptionEvent;
import com.ecommerce.common.exception.ExceptionLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Map;

/**
 * Сервис для публикации событий ошибок в SNS
 */
@Service
@Slf4j
public class ExceptionPublisher {
    
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String exceptionTopicArn;
    private final String serviceName;
    private final String environment;
    
    public ExceptionPublisher(SnsClient snsClient, 
                             ObjectMapper objectMapper,
                             @Value("${aws.sns.exception-topic-arn}") String exceptionTopicArn,
                             @Value("${spring.application.name:ecommerce-backend}") String serviceName,
                             @Value("${spring.profiles.active:dev}") String environment) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.exceptionTopicArn = exceptionTopicArn;
        this.serviceName = serviceName;
        this.environment = environment;
    }
    
    /**
     * Публикует исключение в SNS
     */
    public void publishException(Exception exception) {
        try {
            ExceptionEvent event = ExceptionEvent.fromException(exception, serviceName, environment);
            publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish exception: {}", exception.getClass().getSimpleName(), e);
        }
    }
    
    /**
     * Публикует исключение с дополнительным контекстом
     */
    public void publishException(Exception exception, Map<String, Object> context) {
        try {
            ExceptionEvent event = ExceptionEvent.fromExceptionWithContext(exception, serviceName, environment, context);
            publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish exception with context: {}", exception.getClass().getSimpleName(), e);
        }
    }
    
    /**
     * Публикует исключение с дополнительными параметрами
     */
    public void publishException(Exception exception, ExceptionLevel level, String... tags) {
        try {
            ExceptionEvent event = ExceptionEvent.fromException(exception, serviceName, environment)
                    .withLevel(level);
            
            for (String tag : tags) {
                event.withTag(tag);
            }
            
            publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish exception with level and tags: {}", exception.getClass().getSimpleName(), e);
        }
    }
    
    /**
     * Публикует исключение с полным контролем
     */
    public void publishException(Exception exception, ExceptionLevel level, 
                                Map<String, Object> context, String... tags) {
        try {
            ExceptionEvent event = ExceptionEvent.fromExceptionWithContext(exception, serviceName, environment, context)
                    .withLevel(level);
            
            for (String tag : tags) {
                event.withTag(tag);
            }
            
            publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish exception with full control: {}", exception.getClass().getSimpleName(), e);
        }
    }
    
    /**
     * Публикует кастомное событие ошибки
     */
    public void publishCustomError(String errorType, String message, ExceptionLevel level) {
        try {
            ExceptionEvent event = ExceptionEvent.builder()
                    .service(serviceName)
                    .timestamp(java.time.Instant.now())
                    .exceptionType(errorType)
                    .message(message)
                    .level(level)
                    .environment(environment)
                    .build();
            
            publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish custom error: {}", errorType, e);
        }
    }
    
    /**
     * Публикует событие в SNS
     */
    private void publishEvent(ExceptionEvent event) {
        try {
            // Сериализация события в JSON
            String jsonMessage = objectMapper.writeValueAsString(event);
            
            // Создание запроса для SNS
            PublishRequest request = PublishRequest.builder()
                    .topicArn(exceptionTopicArn)
                    .message(jsonMessage)
                    .subject(String.format("Exception: %s in %s", event.getExceptionType(), event.getService()))
                    .build();
            
            // Публикация в SNS
            PublishResponse response = snsClient.publish(request);
            
            log.debug("Published exception event to SNS: {} (MessageId: {})", 
                    event.getExceptionType(), response.messageId());
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize exception event: {}", event.getExceptionType(), e);
        } catch (Exception e) {
            log.error("Failed to publish exception event to SNS: {}", event.getExceptionType(), e);
        }
    }
    
    /**
     * Проверяет, включена ли публикация ошибок
     */
    public boolean isEnabled() {
        return exceptionTopicArn != null && !exceptionTopicArn.trim().isEmpty();
    }
} 
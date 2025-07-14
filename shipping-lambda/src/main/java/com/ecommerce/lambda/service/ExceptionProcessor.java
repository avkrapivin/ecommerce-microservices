package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.ExceptionEvent;
import com.ecommerce.lambda.model.ExceptionLevel;
import com.ecommerce.lambda.model.ExceptionRecord;
import com.ecommerce.lambda.repository.ExceptionRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Сервис для обработки и группировки ошибок
 */
@Slf4j
public class ExceptionProcessor {
    
    private final ExceptionRepository repository;
    private final MetricsPublisher metricsPublisher;
    
    // Временное окно для группировки (5 минут)
    private static final long GROUPING_WINDOW_MINUTES = 5;
    
    public ExceptionProcessor(ExceptionRepository repository, MetricsPublisher metricsPublisher) {
        this.repository = repository;
        this.metricsPublisher = metricsPublisher;
    }
    
    /**
     * Обрабатывает событие ошибки
     */
    public void processException(ExceptionEvent event) {
        try {
            log.info("Processing exception: {} from service: {}", event.getExceptionType(), event.getService());
            log.debug("Full event details: {}", event);
            
            // Валидация события
            if (!isValidEvent(event)) {
                log.warn("Invalid exception event received: {}", event);
                return;
            }
            log.debug("Event validation passed");
            
            // Фильтрация шума
            if (shouldFilterOut(event)) {
                log.debug("Filtering out exception: {} from service: {}", event.getExceptionType(), event.getService());
                return;
            }
            log.debug("Event passed filtering");
            
            // Попытка найти похожую ошибку для группировки
            String partitionKey = event.getPartitionKey();
            log.debug("Looking for similar exceptions with partition key: {}", partitionKey);
            
            Optional<ExceptionRecord> existingRecord = repository.findSimilarException(
                    partitionKey, 
                    event.getGroupingKey()
            );
            
            if (existingRecord.isPresent()) {
                log.info("Found existing exception record for grouping");
                handleExistingException(existingRecord.get(), event);
            } else {
                log.info("No existing record found, creating new exception");
                handleNewException(event);
            }
            
            // Публикация метрик в CloudWatch
            log.debug("Publishing metrics for exception");
            metricsPublisher.publishExceptionMetrics(event);
            
            // Проверка на необходимость создания алерта
            if (event.getLevel().requiresImmediateAlert()) {
                log.info("Exception requires immediate alert, creating alert");
                createAlert(event);
            }
            
            log.info("Successfully processed exception: {} from service: {}", event.getExceptionType(), event.getService());
            
        } catch (Exception e) {
            log.error("Failed to process exception: {} from service: {}", event.getExceptionType(), event.getService(), e);
            throw e;
        }
    }
    
    /**
     * Валидирует событие ошибки
     */
    private boolean isValidEvent(ExceptionEvent event) {
        return event != null &&
               event.getService() != null && !event.getService().trim().isEmpty() &&
               event.getExceptionType() != null && !event.getExceptionType().trim().isEmpty() &&
               event.getLevel() != null &&
               event.getTimestamp() != null;
    }
    
    /**
     * Проверяет, нужно ли фильтровать ошибку
     */
    private boolean shouldFilterOut(ExceptionEvent event) {
        // Фильтруем TRACE и DEBUG в production
        if ("production".equals(event.getEnvironment()) && 
            (event.getLevel() == ExceptionLevel.TRACE || event.getLevel() == ExceptionLevel.DEBUG)) {
            return true;
        }
        
        // Фильтруем известные временные ошибки
        if (isKnownTemporaryError(event)) {
            return true;
        }
        
        // Фильтруем по blacklist
        if (isBlacklistedError(event)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Проверяет, является ли ошибка известной временной
     */
    private boolean isKnownTemporaryError(ExceptionEvent event) {
        String exceptionType = event.getExceptionType();
        String message = event.getMessage();
        
        // Примеры временных ошибок
        if ("java.net.SocketTimeoutException".equals(exceptionType) ||
            "java.net.ConnectException".equals(exceptionType) ||
            "software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughputExceededException".equals(exceptionType)) {
            return true;
        }
        
        // Фильтруем по сообщениям
        if (message != null) {
            if (message.contains("connection timeout") ||
                message.contains("temporary failure") ||
                message.contains("rate limit exceeded")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Проверяет, находится ли ошибка в blacklist
     */
    private boolean isBlacklistedError(ExceptionEvent event) {
        String exceptionType = event.getExceptionType();
        
        // Blacklist для неважных исключений
        return "java.lang.InterruptedException".equals(exceptionType) ||
               "java.util.concurrent.CancellationException".equals(exceptionType) ||
               "org.springframework.web.context.request.async.AsyncRequestTimeoutException".equals(exceptionType);
    }
    
    /**
     * Обрабатывает новую ошибку
     */
    private void handleNewException(ExceptionEvent event) {
        try {
            ExceptionRecord record = ExceptionRecord.fromEvent(event);
            repository.saveException(record);
            
            log.info("Saved new exception: {} from service: {}", event.getExceptionType(), event.getService());
            
        } catch (Exception e) {
            log.error("Failed to save new exception: {}", event.getExceptionType(), e);
            throw e;
        }
    }
    
    /**
     * Обрабатывает существующую ошибку (группировка)
     */
    private void handleExistingException(ExceptionRecord existingRecord, ExceptionEvent event) {
        try {
            // Проверяем, не слишком ли старая запись для группировки
            Instant existingTime = Instant.parse(existingRecord.getLastOccurrence());
            Instant currentTime = event.getTimestamp();
            
            long minutesDifference = ChronoUnit.MINUTES.between(existingTime, currentTime);
            
            if (minutesDifference <= GROUPING_WINDOW_MINUTES) {
                // Обновляем существующую запись
                existingRecord.setLastOccurrence(currentTime.toString());
                repository.updateExceptionCount(existingRecord);
                
                // Публикуем метрики для группированной ошибки
                metricsPublisher.publishGroupedExceptionMetrics(event, existingRecord.getCount() + 1);
                
                log.info("Updated grouped exception: {} from service: {}, count: {}", 
                        event.getExceptionType(), event.getService(), existingRecord.getCount() + 1);
            } else {
                // Создаем новую запись, если прошло слишком много времени
                handleNewException(event);
            }
            
        } catch (Exception e) {
            log.error("Failed to update existing exception: {}", event.getExceptionType(), e);
            throw e;
        }
    }
    
    /**
     * Создает алерт для критической ошибки
     */
    private void createAlert(ExceptionEvent event) {
        try {
            String alertLevel = event.getLevel().requiresImmediateAlert() ? "CRITICAL" : "WARNING";
            
            log.warn("ALERT [{}]: {} in service: {} - {}", 
                    alertLevel, event.getExceptionType(), event.getService(), event.getMessage());
            
            // Здесь можно добавить интеграцию с системами алертов
            // например, отправку в Slack, PagerDuty, etc.
            
        } catch (Exception e) {
            log.error("Failed to create alert for exception: {}", event.getExceptionType(), e);
        }
    }
} 
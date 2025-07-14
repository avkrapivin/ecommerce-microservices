package com.ecommerce.lambda.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Модель записи ошибки для хранения в DynamoDB
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionRecord {
    
    /**
     * Составной ключ: service#exceptionType
     */
    private String partitionKey;
    
    /**
     * Временная метка в ISO формате
     */
    private String timestamp;
    
    /**
     * Название сервиса
     */
    private String service;
    
    /**
     * Тип исключения
     */
    private String exceptionType;
    
    /**
     * Сообщение об ошибке
     */
    private String message;
    
    /**
     * Stack trace (сокращенный для экономии места)
     */
    private String stackTrace;
    
    /**
     * Контекстная информация в JSON формате
     */
    private Map<String, Object> context;
    
    /**
     * Уровень критичности
     */
    private String level;
    
    /**
     * Окружение
     */
    private String environment;
    
    /**
     * Версия сервиса
     */
    private String version;
    
    /**
     * Теги
     */
    private List<String> tags;
    
    /**
     * Количество одинаковых ошибок (для группировки)
     */
    private Long count;
    
    /**
     * Время первого возникновения ошибки
     */
    private String firstOccurrence;
    
    /**
     * Время последнего возникновения ошибки
     */
    private String lastOccurrence;
    
    /**
     * TTL для автоматического удаления (epoch seconds)
     */
    private Long ttl;
    
    /**
     * Создает ExceptionRecord из ExceptionEvent
     */
    public static ExceptionRecord fromEvent(ExceptionEvent event) {
        Instant now = Instant.now();
        String timestampStr = event.getTimestamp().toString();
        
        return ExceptionRecord.builder()
                .partitionKey(event.getPartitionKey())
                .timestamp(timestampStr)
                .service(event.getService())
                .exceptionType(event.getExceptionType())
                .message(event.getMessage())
                .stackTrace(truncateStackTrace(event.getStackTrace()))
                .context(event.getContext())
                .level(event.getLevel().toString())
                .environment(event.getEnvironment())
                .version(event.getVersion())
                .tags(event.getTags())
                .count(1L)
                .firstOccurrence(timestampStr)
                .lastOccurrence(timestampStr)
                .ttl(now.getEpochSecond() + (30 * 24 * 60 * 60)) // 30 дней TTL
                .build();
    }
    
    /**
     * Преобразует в Map для работы с DynamoDB
     */
    public Map<String, Object> toAttributeMap() {
        Map<String, Object> item = new HashMap<>();
        
        item.put("partitionKey", partitionKey);
        item.put("timestamp", timestamp);
        item.put("service", service);
        item.put("exceptionType", exceptionType);
        item.put("message", message);
        item.put("stackTrace", stackTrace);
        item.put("context", context);
        item.put("level", level);
        item.put("environment", environment);
        item.put("version", version);
        item.put("tags", tags);
        item.put("count", count);
        item.put("firstOccurrence", firstOccurrence);
        item.put("lastOccurrence", lastOccurrence);
        item.put("ttl", ttl);
        
        return item;
    }
    
    /**
     * Сокращает stack trace для экономии места в DynamoDB
     */
    private static String truncateStackTrace(String stackTrace) {
        if (stackTrace == null) return null;
        
        // Ограничиваем размер stack trace до 4KB
        final int MAX_STACK_TRACE_LENGTH = 4000;
        if (stackTrace.length() > MAX_STACK_TRACE_LENGTH) {
            return stackTrace.substring(0, MAX_STACK_TRACE_LENGTH) + "... [truncated]";
        }
        return stackTrace;
    }
} 
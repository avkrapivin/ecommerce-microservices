package com.ecommerce.common.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Модель события ошибки для централизованного мониторинга
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExceptionEvent {
    
    /**
     * Название сервиса, где произошла ошибка
     */
    @JsonProperty("service")
    private String service;
    
    /**
     * Временная метка события
     */
    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;
    
    /**
     * Тип исключения (класс)
     */
    @JsonProperty("exceptionType")
    private String exceptionType;
    
    /**
     * Сообщение об ошибке
     */
    @JsonProperty("message")
    private String message;
    
    /**
     * Полный stack trace
     */
    @JsonProperty("stackTrace")
    private String stackTrace;
    
    /**
     * Контекстная информация
     */
    @JsonProperty("context")
    private Map<String, Object> context;
    
    /**
     * Уровень критичности
     */
    @JsonProperty("level")
    private ExceptionLevel level;
    
    /**
     * Окружение (dev, staging, production)
     */
    @JsonProperty("environment")
    private String environment;
    
    /**
     * Версия сервиса
     */
    @JsonProperty("version")
    private String version;
    
    /**
     * Теги для группировки и фильтрации
     */
    @JsonProperty("tags")
    private List<String> tags;
    
    /**
     * Создает ключ для партиционирования в DynamoDB
     */
    public String getPartitionKey() {
        return service + "#" + exceptionType;
    }
    
    /**
     * Создает ключ для группировки похожих ошибок
     */
    public String getGroupingKey() {
        return service + "#" + exceptionType + "#" + (message != null ? message.hashCode() : 0);
    }
    
    /**
     * Создает ExceptionEvent из Java исключения
     */
    public static ExceptionEvent fromException(Exception exception, String service, String environment) {
        return ExceptionEvent.builder()
                .service(service)
                .timestamp(Instant.now())
                .exceptionType(exception.getClass().getSimpleName())
                .message(exception.getMessage())
                .stackTrace(getStackTraceString(exception))
                .context(new HashMap<>())
                .level(determineLevel(exception))
                .environment(environment)
                .tags(new ArrayList<>())
                .build();
    }
    
    /**
     * Создает ExceptionEvent с дополнительным контекстом
     */
    public static ExceptionEvent fromExceptionWithContext(Exception exception, String service, 
                                                         String environment, Map<String, Object> context) {
        ExceptionEvent event = fromException(exception, service, environment);
        if (context != null) {
            event.setContext(context);
        }
        return event;
    }
    
    /**
     * Добавляет контекстную информацию
     */
    public ExceptionEvent withContext(String key, Object value) {
        if (this.context == null) {
            this.context = new HashMap<>();
        }
        this.context.put(key, value);
        return this;
    }
    
    /**
     * Добавляет тег
     */
    public ExceptionEvent withTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(tag);
        return this;
    }
    
    /**
     * Устанавливает уровень критичности
     */
    public ExceptionEvent withLevel(ExceptionLevel level) {
        this.level = level;
        return this;
    }
    
    /**
     * Устанавливает версию сервиса
     */
    public ExceptionEvent withVersion(String version) {
        this.version = version;
        return this;
    }
    
    /**
     * Преобразует stack trace в строку
     */
    private static String getStackTraceString(Exception exception) {
        if (exception == null) return null;
        
        StringBuilder sb = new StringBuilder();
        sb.append(exception.getClass().getName());
        if (exception.getMessage() != null) {
            sb.append(": ").append(exception.getMessage());
        }
        sb.append("\n");
        
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        // Добавляем caused by, если есть
        Throwable cause = exception.getCause();
        while (cause != null) {
            sb.append("Caused by: ").append(cause.getClass().getName());
            if (cause.getMessage() != null) {
                sb.append(": ").append(cause.getMessage());
            }
            sb.append("\n");
            for (StackTraceElement element : cause.getStackTrace()) {
                sb.append("\tat ").append(element.toString()).append("\n");
            }
            cause = cause.getCause();
        }
        
        return sb.toString();
    }
    
    /**
     * Определяет уровень критичности по типу исключения
     */
    private static ExceptionLevel determineLevel(Exception exception) {
        String className = exception.getClass().getSimpleName();
        
        // FATAL ошибки
        if (className.contains("OutOfMemory") || 
            className.contains("StackOverflow") ||
            className.contains("NoClassDefFound")) {
            return ExceptionLevel.FATAL;
        }
        
        // ERROR ошибки
        if (className.contains("SQL") ||
            className.contains("Connection") ||
            className.contains("Timeout") ||
            className.contains("Security") ||
            className.contains("Authentication") ||
            className.contains("Authorization")) {
            return ExceptionLevel.ERROR;
        }
        
        // WARN ошибки
        if (className.contains("Validation") ||
            className.contains("IllegalArgument") ||
            className.contains("IllegalState")) {
            return ExceptionLevel.WARN;
        }
        
        // По умолчанию ERROR
        return ExceptionLevel.ERROR;
    }
} 
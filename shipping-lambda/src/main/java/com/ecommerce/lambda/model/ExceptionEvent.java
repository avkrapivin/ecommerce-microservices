package com.ecommerce.lambda.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
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
     * Идентификатор корреляции для отслеживания запросов
     */
    @JsonProperty("correlationId")
    private String correlationId;
    
    /**
     * Идентификатор пользователя
     */
    @JsonProperty("userId")
    private String userId;
    
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
} 
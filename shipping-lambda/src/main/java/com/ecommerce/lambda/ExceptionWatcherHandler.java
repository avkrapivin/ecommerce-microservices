package com.ecommerce.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.ecommerce.lambda.model.ExceptionEvent;
import com.ecommerce.lambda.repository.ExceptionRepository;
import com.ecommerce.lambda.service.ExceptionProcessor;
import com.ecommerce.lambda.service.MetricsPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Lambda handler для обработки событий ошибок из SNS
 */
@Slf4j
public class ExceptionWatcherHandler implements RequestHandler<SNSEvent, String> {
    
    private final ExceptionProcessor exceptionProcessor;
    private final ObjectMapper objectMapper;
    
    public ExceptionWatcherHandler() {
        // Инициализация AWS клиентов
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        CloudWatchClient cloudWatchClient = CloudWatchClient.builder().build();
        
        // Получение переменных окружения (поддержка как env так и system properties для тестов)
        String tableName = System.getenv("EXCEPTIONS_TABLE_NAME");
        if (tableName == null) {
            tableName = System.getProperty("EXCEPTIONS_TABLE_NAME");
        }
        
        String metricsNamespace = System.getenv("METRICS_NAMESPACE");
        if (metricsNamespace == null) {
            metricsNamespace = System.getProperty("METRICS_NAMESPACE");
        }
        if (metricsNamespace == null) {
            metricsNamespace = "ECommerce/Exceptions";
        }
        
        // Добавим логирование для диагностики
        System.out.println("ExceptionWatcherHandler initialized with:");
        System.out.println("- Table name: " + tableName);
        System.out.println("- Metrics namespace: " + metricsNamespace);
        
        // Инициализация сервисов
        ExceptionRepository repository = new ExceptionRepository(dynamoDbClient, tableName);
        MetricsPublisher metricsPublisher = new MetricsPublisher(cloudWatchClient, metricsNamespace);
        this.exceptionProcessor = new ExceptionProcessor(repository, metricsPublisher);
        
        // Настройка ObjectMapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    // Конструктор для тестирования
    public ExceptionWatcherHandler(ExceptionProcessor exceptionProcessor, ObjectMapper objectMapper) {
        this.exceptionProcessor = exceptionProcessor;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public String handleRequest(SNSEvent event, Context context) {
        log.info("Processing SNS event with {} records", event.getRecords().size());
        
        int processedCount = 0;
        int errorCount = 0;
        
        for (SNSEvent.SNSRecord record : event.getRecords()) {
            try {
                boolean wasProcessed = processRecord(record);
                if (wasProcessed) {
                    processedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to process SNS record: {}", record.getSNS().getMessageId(), e);
                errorCount++;
            }
        }
        
        String result = String.format("Processed: %d, Errors: %d", processedCount, errorCount);
        log.info("ExceptionWatcher completed: {}", result);
        
        return result;
    }
    
    /**
     * Обрабатывает отдельную запись SNS
     * @return true если сообщение было обработано, false если пропущено
     */
    private boolean processRecord(SNSEvent.SNSRecord record) {
        String message = record.getSNS().getMessage();
        log.debug("Processing message: {}", message);
        
        // Проверка на null или пустое сообщение
        if (message == null || message.trim().isEmpty()) {
            log.warn("Received null or empty message, skipping processing");
            return false; // Не считается ни обработанным, ни ошибкой
        }
        
        try {
            // Десериализация события ошибки
            ExceptionEvent exceptionEvent = objectMapper.readValue(message, ExceptionEvent.class);
            
            // Валидация базовых полей
            if (exceptionEvent == null) {
                log.warn("Received null exception event");
                return false;
            }
            
            // Обработка события
            exceptionProcessor.processException(exceptionEvent);
            
            log.debug("Successfully processed exception: {} from service: {}", 
                    exceptionEvent.getExceptionType(), exceptionEvent.getService());
            
            return true; // Успешно обработано
            
        } catch (Exception e) {
            log.error("Failed to process SNS record: {}", record.getSNS().getMessageId(), e);
            throw new RuntimeException("Failed to process SNS record", e);
        }
    }
} 
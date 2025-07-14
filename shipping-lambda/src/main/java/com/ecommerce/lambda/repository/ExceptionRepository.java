package com.ecommerce.lambda.repository;

import com.ecommerce.lambda.model.ExceptionRecord;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Репозиторий для работы с ошибками в DynamoDB
 */
@Slf4j
public class ExceptionRepository {
    
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    
    public ExceptionRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }
    
    /**
     * Сохраняет новую ошибку в DynamoDB
     */
    public void saveException(ExceptionRecord record) {
        try {
            log.info("Attempting to save exception record: {}", record.getPartitionKey());
            log.debug("Full record details: {}", record);
            
            Map<String, AttributeValue> item = convertToAttributeValueMap(record);
            log.debug("Converted to DynamoDB item with {} attributes", item.size());
            
            if (tableName == null) {
                throw new IllegalStateException("Table name is null - cannot save exception");
            }
            
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();
            
            log.debug("Calling DynamoDB putItem for table: {}", tableName);
            PutItemResponse response = dynamoDbClient.putItem(request);
            log.info("Successfully saved exception record: {} to table: {}", record.getPartitionKey(), tableName);
            log.debug("DynamoDB response: {}", response);
            
        } catch (Exception e) {
            log.error("Failed to save exception record: {} to table: {}", record.getPartitionKey(), tableName, e);
            throw new RuntimeException("Failed to save exception record", e);
        }
    }
    
    /**
     * Проверяет, существует ли похожая ошибка (для группировки)
     */
    public Optional<ExceptionRecord> findSimilarException(String partitionKey, String groupingKey) {
        try {
            // Ищем записи с тем же partitionKey за последние 5 минут
            Map<String, AttributeValue> keyConditions = new HashMap<>();
            keyConditions.put("partitionKey", AttributeValue.builder().s(partitionKey).build());
            
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("partitionKey = :pk")
                    .expressionAttributeValues(Map.of(":pk", AttributeValue.builder().s(partitionKey).build()))
                    .scanIndexForward(false) // Сортировка по убыванию (новые записи первыми)
                    .limit(10) // Ограничиваем количество для производительности
                    .build();
            
            QueryResponse response = dynamoDbClient.query(request);
            
            // Ищем похожую ошибку среди последних записей
            for (Map<String, AttributeValue> item : response.items()) {
                ExceptionRecord record = convertFromAttributeValueMap(item);
                if (isSimilarException(record, groupingKey)) {
                    return Optional.of(record);
                }
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            log.error("Failed to find similar exception for partitionKey: {}", partitionKey, e);
            return Optional.empty();
        }
    }
    
    /**
     * Обновляет существующую ошибку (увеличивает счетчик)
     */
    public void updateExceptionCount(ExceptionRecord record) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("partitionKey", AttributeValue.builder().s(record.getPartitionKey()).build());
            key.put("timestamp", AttributeValue.builder().s(record.getTimestamp()).build());
            
            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .updateExpression("SET #count = #count + :inc, lastOccurrence = :lastOcc")
                    .expressionAttributeNames(Map.of("#count", "count"))
                    .expressionAttributeValues(Map.of(
                            ":inc", AttributeValue.builder().n("1").build(),
                            ":lastOcc", AttributeValue.builder().s(record.getLastOccurrence()).build()
                    ))
                    .build();
            
            dynamoDbClient.updateItem(request);
            log.debug("Updated exception count for: {}", record.getPartitionKey());
            
        } catch (Exception e) {
            log.error("Failed to update exception count: {}", record.getPartitionKey(), e);
            throw new RuntimeException("Failed to update exception count", e);
        }
    }
    
    /**
     * Преобразует ExceptionRecord в Map<String, AttributeValue>
     */
    private Map<String, AttributeValue> convertToAttributeValueMap(ExceptionRecord record) {
        Map<String, AttributeValue> item = new HashMap<>();
        
        item.put("partitionKey", AttributeValue.builder().s(record.getPartitionKey()).build());
        item.put("timestamp", AttributeValue.builder().s(record.getTimestamp()).build());
        item.put("service", AttributeValue.builder().s(record.getService()).build());
        item.put("exceptionType", AttributeValue.builder().s(record.getExceptionType()).build());
        item.put("message", AttributeValue.builder().s(record.getMessage() != null ? record.getMessage() : "").build());
        item.put("level", AttributeValue.builder().s(record.getLevel()).build());
        item.put("environment", AttributeValue.builder().s(record.getEnvironment() != null ? record.getEnvironment() : "unknown").build());
        item.put("count", AttributeValue.builder().n(String.valueOf(record.getCount())).build());
        item.put("firstOccurrence", AttributeValue.builder().s(record.getFirstOccurrence()).build());
        item.put("lastOccurrence", AttributeValue.builder().s(record.getLastOccurrence()).build());
        item.put("ttl", AttributeValue.builder().n(String.valueOf(record.getTtl())).build());
        
        if (record.getStackTrace() != null) {
            item.put("stackTrace", AttributeValue.builder().s(record.getStackTrace()).build());
        }
        
        if (record.getVersion() != null) {
            item.put("version", AttributeValue.builder().s(record.getVersion()).build());
        }
        
        if (record.getContext() != null && !record.getContext().isEmpty()) {
            item.put("context", AttributeValue.builder().s(record.getContext().toString()).build());
        }
        
        if (record.getTags() != null && !record.getTags().isEmpty()) {
            item.put("tags", AttributeValue.builder().ss(record.getTags()).build());
        }
        
        return item;
    }
    
    /**
     * Преобразует Map<String, AttributeValue> в ExceptionRecord
     */
    private ExceptionRecord convertFromAttributeValueMap(Map<String, AttributeValue> item) {
        return ExceptionRecord.builder()
                .partitionKey(item.get("partitionKey").s())
                .timestamp(item.get("timestamp").s())
                .service(item.get("service").s())
                .exceptionType(item.get("exceptionType").s())
                .message(item.get("message").s())
                .level(item.get("level").s())
                .environment(item.get("environment").s())
                .count(Long.parseLong(item.get("count").n()))
                .firstOccurrence(item.get("firstOccurrence").s())
                .lastOccurrence(item.get("lastOccurrence").s())
                .ttl(Long.parseLong(item.get("ttl").n()))
                .stackTrace(item.containsKey("stackTrace") ? item.get("stackTrace").s() : null)
                .version(item.containsKey("version") ? item.get("version").s() : null)
                .build();
    }
    
    /**
     * Проверяет, является ли ошибка похожей (для группировки)
     */
    private boolean isSimilarException(ExceptionRecord record, String groupingKey) {
        String recordGroupingKey = record.getService() + "#" + record.getExceptionType() + "#" + 
                (record.getMessage() != null ? record.getMessage().hashCode() : 0);
        return recordGroupingKey.equals(groupingKey);
    }
} 
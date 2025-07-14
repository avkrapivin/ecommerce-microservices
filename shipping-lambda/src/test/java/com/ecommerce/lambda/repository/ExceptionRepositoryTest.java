package com.ecommerce.lambda.repository;

import com.ecommerce.lambda.model.ExceptionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExceptionRepositoryTest {

    @Mock
    private DynamoDbClient dynamoDbClient;

    private ExceptionRepository repository;
    private final String tableName = "test-exceptions-table";

    @BeforeEach
    void setUp() {
        repository = new ExceptionRepository(dynamoDbClient, tableName);
    }

    @Test
    void shouldSaveExceptionSuccessfully() {
        // Given
        ExceptionRecord record = createTestRecord();
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // When
        repository.saveException(record);

        // Then
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDbClient).putItem(captor.capture());
        
        PutItemRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo(tableName);
        assertThat(request.item()).containsKey("partitionKey");
        assertThat(request.item()).containsKey("timestamp");
        assertThat(request.item().get("service").s()).isEqualTo("test-service");
    }

    @Test
    void shouldThrowExceptionWhenSaveFails() {
        // Given
        ExceptionRecord record = createTestRecord();
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("DynamoDB error").build());

        // When & Then
        assertThatThrownBy(() -> repository.saveException(record))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save exception record");
    }

    @Test
    void shouldFindSimilarExceptionWhenExists() {
        // Given
        String partitionKey = "test-service#NullPointerException";
        String testMessage = "Test exception message";
        String groupingKey = "test-service#NullPointerException#" + testMessage.hashCode();
        
        Map<String, AttributeValue> item = createDynamoDbItem();
        QueryResponse response = QueryResponse.builder()
                .items(List.of(item))
                .build();
        
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

        // When
        Optional<ExceptionRecord> result = repository.findSimilarException(partitionKey, groupingKey);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getService()).isEqualTo("test-service");
        assertThat(result.get().getExceptionType()).isEqualTo("NullPointerException");
    }

    @Test
    void shouldReturnEmptyWhenNoSimilarExceptionFound() {
        // Given
        String partitionKey = "test-service#NullPointerException";
        String groupingKey = "test-service#NullPointerException#54321";
        
        QueryResponse response = QueryResponse.builder()
                .items(List.of())
                .build();
        
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(response);

        // When
        Optional<ExceptionRecord> result = repository.findSimilarException(partitionKey, groupingKey);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleQueryExceptionGracefully() {
        // Given
        String partitionKey = "test-service#NullPointerException";
        String groupingKey = "test-service#NullPointerException#12345";
        
        when(dynamoDbClient.query(any(QueryRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Query failed").build());

        // When
        Optional<ExceptionRecord> result = repository.findSimilarException(partitionKey, groupingKey);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldUpdateExceptionCountSuccessfully() {
        // Given
        ExceptionRecord record = createTestRecord();
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenReturn(UpdateItemResponse.builder().build());

        // When
        repository.updateExceptionCount(record);

        // Then
        ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
        verify(dynamoDbClient).updateItem(captor.capture());
        
        UpdateItemRequest request = captor.getValue();
        assertThat(request.tableName()).isEqualTo(tableName);
        assertThat(request.updateExpression()).contains("SET #count = #count + :inc");
        assertThat(request.expressionAttributeNames()).containsEntry("#count", "count");
    }

    @Test
    void shouldThrowExceptionWhenUpdateFails() {
        // Given
        ExceptionRecord record = createTestRecord();
        when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
                .thenThrow(DynamoDbException.builder().message("Update failed").build());

        // When & Then
        assertThatThrownBy(() -> repository.updateExceptionCount(record))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to update exception count");
    }

    @Test
    void shouldConvertAttributeValueMapCorrectly() {
        // Given
        ExceptionRecord record = createTestRecord();
        when(dynamoDbClient.putItem(any(PutItemRequest.class)))
                .thenReturn(PutItemResponse.builder().build());

        // When
        repository.saveException(record);

        // Then
        ArgumentCaptor<PutItemRequest> captor = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamoDbClient).putItem(captor.capture());
        
        Map<String, AttributeValue> item = captor.getValue().item();
        assertThat(item.get("partitionKey").s()).isEqualTo(record.getPartitionKey());
        assertThat(item.get("timestamp").s()).isEqualTo(record.getTimestamp());
        assertThat(item.get("service").s()).isEqualTo(record.getService());
        assertThat(item.get("exceptionType").s()).isEqualTo(record.getExceptionType());
        assertThat(item.get("count").n()).isEqualTo("1");
        assertThat(item.get("ttl").n()).isEqualTo(String.valueOf(record.getTtl()));
    }

    private ExceptionRecord createTestRecord() {
        return ExceptionRecord.builder()
                .partitionKey("test-service#NullPointerException")
                .timestamp("2024-01-01T12:00:00Z")
                .service("test-service")
                .exceptionType("NullPointerException")
                .message("Test exception message")
                .stackTrace("java.lang.NullPointerException\n\tat test.method(Test.java:1)")
                .level("ERROR")
                .environment("test")
                .version("1.0.0")
                .count(1L)
                .firstOccurrence("2024-01-01T12:00:00Z")
                .lastOccurrence("2024-01-01T12:00:00Z")
                .ttl(1704110400L)
                .build();
    }

    private Map<String, AttributeValue> createDynamoDbItem() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("partitionKey", AttributeValue.builder().s("test-service#NullPointerException").build());
        item.put("timestamp", AttributeValue.builder().s("2024-01-01T12:00:00Z").build());
        item.put("service", AttributeValue.builder().s("test-service").build());
        item.put("exceptionType", AttributeValue.builder().s("NullPointerException").build());
        item.put("message", AttributeValue.builder().s("Test exception message").build());
        item.put("level", AttributeValue.builder().s("ERROR").build());
        item.put("environment", AttributeValue.builder().s("test").build());
        item.put("count", AttributeValue.builder().n("1").build());
        item.put("firstOccurrence", AttributeValue.builder().s("2024-01-01T12:00:00Z").build());
        item.put("lastOccurrence", AttributeValue.builder().s("2024-01-01T12:00:00Z").build());
        item.put("ttl", AttributeValue.builder().n("1704110400").build());
        return item;
    }
} 
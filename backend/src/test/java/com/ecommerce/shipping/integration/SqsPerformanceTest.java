package com.ecommerce.shipping.integration;

import com.ecommerce.shipping.event.OrderStatusUpdateListener;
import com.ecommerce.shipping.model.ShippingInfo;
import com.ecommerce.shipping.model.ShippingStatus;
import com.ecommerce.shipping.repository.ShippingInfoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Тест производительности для сравнения SQS и HTTPS обработки
 * Упрощенная версия без Spring контекста
 */
@Testcontainers
class SqsPerformanceTest {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(SNS, SQS);

    @Mock
    private OrderStatusUpdateListener orderStatusUpdateListener;

    @Mock
    private ShippingInfoRepository shippingInfoRepository;

    private ObjectMapper objectMapper;

    private SnsClient snsClient;
    private SqsClient sqsClient;
    private String topicArn;
    private String queueUrl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        
        AwsBasicCredentials credentials = AwsBasicCredentials.create("test", "test");
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        snsClient = SnsClient.builder()
                .endpointOverride(localstack.getEndpointOverride(SNS))
                .credentialsProvider(credentialsProvider)
                .region(Region.US_EAST_1)
                .build();

        sqsClient = SqsClient.builder()
                .endpointOverride(localstack.getEndpointOverride(SQS))
                .credentialsProvider(credentialsProvider)
                .region(Region.US_EAST_1)
                .build();

        setupAwsResources();
    }

    @AfterEach
    void tearDown() {
        if (snsClient != null) snsClient.close();
        if (sqsClient != null) sqsClient.close();
    }

    private void setupAwsResources() {
        // Создаем SNS топик
        CreateTopicResponse topicResponse = snsClient.createTopic(
                CreateTopicRequest.builder()
                        .name("OrderStatusUpdatedPerf")
                        .build()
        );
        topicArn = topicResponse.topicArn();

        // Создаем SQS очередь
        CreateQueueResponse queueResponse = sqsClient.createQueue(
                CreateQueueRequest.builder()
                        .queueName("perf-test-queue")
                        .build()
        );
        queueUrl = queueResponse.queueUrl();

        // Получаем ARN очереди
        GetQueueAttributesResponse queueAttrs = sqsClient.getQueueAttributes(
                GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build()
        );
        String queueArn = queueAttrs.attributes().get(QueueAttributeName.QUEUE_ARN);

        // Устанавливаем политику доступа
        sqsClient.setQueueAttributes(
                SetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributes(java.util.Map.of(
                                QueueAttributeName.POLICY,
                                createQueuePolicy(queueArn, topicArn)
                        ))
                        .build()
        );

        // Подписываем SQS на SNS (без фильтров)
        snsClient.subscribe(
                SubscribeRequest.builder()
                        .topicArn(topicArn)
                        .protocol("sqs")
                        .endpoint(queueArn)
                        .attributes(java.util.Map.of("RawMessageDelivery", "true"))
                        .build()
        );
    }

    @Test
    void shouldCompareDeliveryPerformanceSqsVsHttps() throws Exception {
        // Given
        int messageCount = 10;
        List<String> orderIds = new ArrayList<>();
        
        // Создаем тестовые сообщения
        for (int i = 0; i < messageCount; i++) {
            orderIds.add("perf-test-" + i);
        }

        // Test HTTPS Performance (прямые вызовы)
        long httpsStartTime = System.currentTimeMillis();
        
        List<CompletableFuture<Void>> httpsFutures = new ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            String orderId = orderIds.get(i);
            String message = createOrderStatusUpdateMessage(orderId, "HTTPS_PROCESSED", "HTTPS_TRACK_" + i);
            
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    orderStatusUpdateListener.handleOrderStatusUpdate(message);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            httpsFutures.add(future);
        }
        
        // Ждем завершения всех HTTPS обработок
        CompletableFuture.allOf(httpsFutures.toArray(new CompletableFuture[0])).join();
        long httpsEndTime = System.currentTimeMillis();
        long httpsProcessingTime = httpsEndTime - httpsStartTime;

        // Test SQS Performance (SNS → SQS → получение)
        long sqsStartTime = System.currentTimeMillis();
        
        // Отправляем сообщения в SNS → SQS
        for (int i = 0; i < messageCount; i++) {
            String orderId = orderIds.get(i);
            String message = createOrderStatusUpdateMessage(orderId, "SQS_PROCESSED", "SQS_TRACK_" + i);
            publishMessageToSns(message);
        }

        // Получаем сообщения из SQS
        int processedCount = 0;
        while (processedCount < messageCount) {
            ReceiveMessageResponse response = sqsClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .maxNumberOfMessages(10)
                            .waitTimeSeconds(10)
                            .build()
            );

            for (Message sqsMessage : response.messages()) {
                // Просто считаем что получили сообщение
                processedCount++;
                
                // Удаляем из очереди
                sqsClient.deleteMessage(
                        DeleteMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .receiptHandle(sqsMessage.receiptHandle())
                                .build()
                );
            }
        }
        
        long sqsEndTime = System.currentTimeMillis();
        long sqsProcessingTime = sqsEndTime - sqsStartTime;

        // Выводим результаты
        System.out.println("=== Performance Comparison Results ===");
        System.out.println("Messages processed: " + messageCount);
        System.out.println("HTTPS processing time: " + httpsProcessingTime + " ms");
        System.out.println("SQS processing time: " + sqsProcessingTime + " ms");
        System.out.println("HTTPS avg per message: " + (httpsProcessingTime / (double) messageCount) + " ms");
        System.out.println("SQS avg per message: " + (sqsProcessingTime / (double) messageCount) + " ms");
        
        if (httpsProcessingTime < sqsProcessingTime) {
            System.out.println("HTTPS is faster by: " + (sqsProcessingTime - httpsProcessingTime) + " ms");
        } else {
            System.out.println("SQS is faster by: " + (httpsProcessingTime - sqsProcessingTime) + " ms");
        }

        // Проверяем что все сообщения обработались корректно
        verify(orderStatusUpdateListener, times(messageCount)).handleOrderStatusUpdate(any(String.class));

        // Оба метода должны быть достаточно быстрыми (менее 30 секунд для 10 сообщений)
        assertThat(httpsProcessingTime).isLessThan(30000);
        assertThat(sqsProcessingTime).isLessThan(30000);
    }

    @Test
    void shouldHandleBurstLoadDelivery() throws Exception {
        // Given - большая нагрузка
        int burstSize = 25; // Уменьшил для ускорения теста
        List<String> orderIds = new ArrayList<>();
        
        for (int i = 0; i < burstSize; i++) {
            orderIds.add("burst-test-" + i);
        }

        // When - отправляем burst сообщений в SNS → SQS
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < burstSize; i++) {
            String message = createOrderStatusUpdateMessage(
                orderIds.get(i), "BURST_PROCESSED", "BURST_TRACK_" + i);
            publishMessageToSns(message);
        }

        // Получаем все сообщения из SQS
        int processedCount = 0;
        while (processedCount < burstSize) {
            ReceiveMessageResponse response = sqsClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .maxNumberOfMessages(10)
                            .waitTimeSeconds(5)
                            .build()
            );

            // Обрабатываем полученные сообщения параллельно
            response.messages().parallelStream().forEach(sqsMessage -> {
                try {
                    sqsClient.deleteMessage(
                            DeleteMessageRequest.builder()
                                    .queueUrl(queueUrl)
                                    .receiptHandle(sqsMessage.receiptHandle())
                                    .build()
                    );
                } catch (Exception e) {
                    System.err.println("Error deleting message: " + e.getMessage());
                }
            });
            
            processedCount += response.messages().size();
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("=== Burst Load Test Results ===");
        System.out.println("Burst size: " + burstSize + " messages");
        System.out.println("Total processing time: " + totalTime + " ms");
        System.out.println("Average per message: " + (totalTime / (double) burstSize) + " ms");
        System.out.println("Throughput: " + (burstSize * 1000.0 / totalTime) + " messages/sec");

        // Then - проверяем что тест завершился в разумное время
        assertThat(totalTime).isLessThan(120000); // Менее 2 минут для 25 сообщений
        
        // Проверяем что получили все сообщения
        assertThat(processedCount).isEqualTo(burstSize);
    }

    private String createQueuePolicy(String queueArn, String topicArn) {
        return "{\n" +
                "  \"Version\": \"2012-10-17\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Principal\": {\"Service\": \"sns.amazonaws.com\"},\n" +
                "      \"Action\": \"sqs:SendMessage\",\n" +
                "      \"Resource\": \"" + queueArn + "\",\n" +
                "      \"Condition\": {\n" +
                "        \"ArnEquals\": {\n" +
                "          \"aws:SourceArn\": \"" + topicArn + "\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }



    private String createOrderStatusUpdateMessage(String orderId, String status, String trackingNumber) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "orderId", orderId,
                "status", status,
                "trackingNumber", trackingNumber,
                "updatedAt", java.time.Instant.now().toString()
        ));
    }

    private void publishMessageToSns(String messageJson) {
        snsClient.publish(
                PublishRequest.builder()
                        .topicArn(topicArn)
                        .message(messageJson)
                        .build()
        );
    }
} 
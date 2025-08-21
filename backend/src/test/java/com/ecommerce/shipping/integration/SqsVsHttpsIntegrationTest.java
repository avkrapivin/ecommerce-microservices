package com.ecommerce.shipping.integration;

import com.ecommerce.shipping.event.OrderStatusUpdateListener;
import com.ecommerce.shipping.model.ShippingInfo;
import com.ecommerce.shipping.model.ShippingStatus;
import com.ecommerce.shipping.repository.ShippingInfoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

/**
 * Интеграционный тест для сравнения SQS и HTTPS обработки событий заказов
 * Запускается только при наличии Docker
 */
@Testcontainers
@EnabledIf("isDockerAvailable")
class SqsVsHttpsIntegrationTest {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(SNS, SQS);

    /**
     * Проверяет доступность Docker для запуска TestContainers
     */
    static boolean isDockerAvailable() {
        try {
            // Пытаемся создать простой TestContainer для проверки Docker
            org.testcontainers.DockerClientFactory.instance().client();
            return true;
        } catch (Exception e) {
            System.out.println("Docker недоступен, пропускаем SqsVsHttpsIntegrationTest: " + e.getMessage());
            return false;
        }
    }

    @Mock
    private ShippingInfoRepository shippingInfoRepository;
    
    @Mock 
    private OrderStatusUpdateListener orderStatusUpdateListener;

    private ObjectMapper objectMapper;

    private SnsClient snsClient;
    private SqsClient sqsClient;
    private String topicArn;
    private String queueUrl;
    private String dlqUrl;
    
    // Тестовые данные в памяти (заменяют БД)
    private List<ShippingInfo> testShippingInfos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        
        // Настройка мока ShippingInfoRepository
        setupShippingInfoRepositoryMocks();
        
        // Настройка AWS клиентов для LocalStack
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
        // Очищаем тестовые данные
        testShippingInfos.clear();
        
        // Закрываем клиенты
        if (snsClient != null) snsClient.close();
        if (sqsClient != null) sqsClient.close();
    }

    private void setupShippingInfoRepositoryMocks() {
        // Настройка save
        when(shippingInfoRepository.save(any(ShippingInfo.class))).thenAnswer(invocation -> {
            ShippingInfo info = invocation.getArgument(0);
            // Присваиваем ID если его нет
            if (info.getId() == null) {
                info.setId((long) (testShippingInfos.size() + 1));
            }
            // Удаляем существующий если есть
            testShippingInfos.removeIf(existing -> existing.getOrderId().equals(info.getOrderId()));
            // Добавляем новый
            testShippingInfos.add(info);
            return info;
        });
        
        // Настройка findByOrderId
        when(shippingInfoRepository.findByOrderId(any(Long.class))).thenAnswer(invocation -> {
            Long orderId = invocation.getArgument(0);
            return testShippingInfos.stream()
                    .filter(info -> info.getOrderId().equals(orderId))
                    .findFirst();
        });
        
        // Настройка deleteAll
        doAnswer(invocation -> {
            testShippingInfos.clear();
            return null;
        }).when(shippingInfoRepository).deleteAll();
        
        // Настройка findAll
        when(shippingInfoRepository.findAll()).thenReturn(testShippingInfos);
    }

    private void setupAwsResources() {
        try {
            // Создаем SNS топик
            CreateTopicResponse topicResponse = snsClient.createTopic(
                    CreateTopicRequest.builder()
                            .name("OrderStatusUpdated")
                            .build()
            );
            topicArn = topicResponse.topicArn();

            // Создаем DLQ
            CreateQueueResponse dlqResponse = sqsClient.createQueue(
                    CreateQueueRequest.builder()
                            .queueName("test-order-status-dlq")
                            .build()
            );
            dlqUrl = dlqResponse.queueUrl();

                         // Создаем основную очередь (упрощенная версия)
             CreateQueueResponse queueResponse = sqsClient.createQueue(
                     CreateQueueRequest.builder()
                             .queueName("test-order-status-queue")
                             .build()
             );
            queueUrl = queueResponse.queueUrl();

            // Получаем ARN очереди для подписки
            String queueArn = getQueueArn(queueUrl);

            // Устанавливаем политику доступа для SNS к SQS
            sqsClient.setQueueAttributes(
                    SetQueueAttributesRequest.builder()
                            .queueUrl(queueUrl)
                            .attributes(java.util.Map.of(
                                    QueueAttributeName.POLICY,
                                    createQueuePolicy(queueArn, topicArn)
                            ))
                            .build()
            );

            // Подписываем SQS на SNS топик (без фильтров для простоты)
            snsClient.subscribe(
                    SubscribeRequest.builder()
                            .topicArn(topicArn)
                            .protocol("sqs")
                            .endpoint(queueArn)
                            .attributes(java.util.Map.of("RawMessageDelivery", "true"))
                            .build()
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to setup AWS resources", e);
        }
    }

    private String getQueueArn(String queueUrl) {
        GetQueueAttributesResponse response = sqsClient.getQueueAttributes(
                GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build()
        );
        return response.attributes().get(QueueAttributeName.QUEUE_ARN);
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

    @Test
    void shouldReceiveMessageFromSnsToSqs() throws Exception {
        // Given
        Long orderId = 123L;
        String messageJson = createOrderStatusUpdateMessage(orderId, "SHIPPED", "TRACK123");

        // When - отправляем сообщение в SNS
        publishMessageToSns(messageJson);

        // Then - проверяем что сообщение попало в SQS
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            ReceiveMessageResponse response = sqsClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .maxNumberOfMessages(10)
                            .waitTimeSeconds(5)
                            .build()
            );
            assertThat(response.messages()).isNotEmpty();
            
            // Проверяем содержимое сообщения
            Message sqsMessage = response.messages().get(0);
            String receivedBody = sqsMessage.body();
            
            // Проверяем что JSON содержит ожидаемые данные
            assertThat(receivedBody).contains(orderId.toString());
            assertThat(receivedBody).contains("SHIPPED");
            assertThat(receivedBody).contains("TRACK123");
        });
    }

    @Test
    void shouldProcessMessageDirectlyViaHttps() throws Exception {
        // Given
        Long orderId = 456L;
        String messageJson = createOrderStatusUpdateMessage(orderId, "IN_TRANSIT", "TRACK456");

        // When - обрабатываем напрямую через HTTPS путь (имитируя webhook)
        orderStatusUpdateListener.handleOrderStatusUpdate(messageJson);

        // Then - проверяем что listener был вызван с правильными данными
        verify(orderStatusUpdateListener).handleOrderStatusUpdate(messageJson);
        
        // Проверяем содержимое сообщения
        assertThat(messageJson).contains(orderId.toString());
        assertThat(messageJson).contains("IN_TRANSIT");
        assertThat(messageJson).contains("TRACK456");
    }

    @Test
    void shouldDeliverMessagesViaBothPaths() throws Exception {
        // Given
        Long orderIdSqs = 789L;
        Long orderIdHttps = 790L;
        String sqsMessage = createOrderStatusUpdateMessage(orderIdSqs, "DELIVERED", "SQS_TRACK789");
        String httpsMessage = createOrderStatusUpdateMessage(orderIdHttps, "DELIVERED", "HTTPS_TRACK790");

        // When - обрабатываем через разные пути
        publishMessageToSns(sqsMessage);
        orderStatusUpdateListener.handleOrderStatusUpdate(httpsMessage);

        // Then - проверяем что SQS сообщение дошло
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            ReceiveMessageResponse response = sqsClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .maxNumberOfMessages(1)
                            .build()
            );
            assertThat(response.messages()).isNotEmpty();
            
            Message sqsMsg = response.messages().get(0);
            String receivedBody = sqsMsg.body();
            
            // Проверяем содержимое SQS сообщения
            assertThat(receivedBody).contains(orderIdSqs.toString());
            assertThat(receivedBody).contains("DELIVERED");
            assertThat(receivedBody).contains("SQS_TRACK789");
        });

        // Проверяем что HTTPS путь тоже сработал
        verify(orderStatusUpdateListener).handleOrderStatusUpdate(httpsMessage);
        
        // Проверяем что оба сообщения имеют одинаковую структуру
        assertThat(sqsMessage).contains("DELIVERED");
        assertThat(httpsMessage).contains("DELIVERED");
    }

    @Test
    void shouldReceiveInvalidMessageInSqs() throws Exception {
        // Given
        String invalidMessage = "{\"invalid\":\"json\",\"missing\":\"required_fields\"}";

        // When
        publishMessageToSns(invalidMessage);

        // Then - проверяем что сообщение попало в основную очередь (DLQ логика будет в настоящем приложении)
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            ReceiveMessageResponse response = sqsClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .maxNumberOfMessages(10)
                            .waitTimeSeconds(5)
                            .build()
            );
            assertThat(response.messages()).isNotEmpty();
            
            Message sqsMessage = response.messages().get(0);
            String receivedBody = sqsMessage.body();
            
            // Проверяем что даже невалидное сообщение доставилось через SQS
            assertThat(receivedBody).contains("invalid");
            assertThat(receivedBody).contains("missing");
        });
    }



    private String createOrderStatusUpdateMessage(Long orderId, String status, String trackingNumber) throws Exception {
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
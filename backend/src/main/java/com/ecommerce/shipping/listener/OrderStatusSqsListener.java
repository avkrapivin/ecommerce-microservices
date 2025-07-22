package com.ecommerce.shipping.listener;

import com.ecommerce.shipping.event.OrderStatusUpdateListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

@Service
@Profile("!test")
@Slf4j
public class OrderStatusSqsListener {

    private final OrderStatusUpdateListener orderStatusUpdateListener;
    private final SqsClient sqsClient;
    private final String queueUrl;

    public OrderStatusSqsListener(
            OrderStatusUpdateListener orderStatusUpdateListener,
            SqsClient sqsClient,
            @Qualifier("orderStatusUpdateQueueUrl") String queueUrl) {
        this.orderStatusUpdateListener = orderStatusUpdateListener;
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Scheduled(fixedDelay = 5000) // Опрос каждые 5 секунд
    @Async
    public void pollMessages() {
        try {
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(20) // Long polling
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);
            List<Message> messages = response.messages();

            if (!messages.isEmpty()) {
                log.info("Received {} SQS messages", messages.size());
                
                // Параллельная обработка сообщений
                messages.parallelStream().forEach(message -> {
                    try {
                        processMessage(message);
                        deleteMessage(message);
                    } catch (Exception e) {
                        log.error("Failed to process SQS message: {}", message.messageId(), e);
                        // Сообщение останется в очереди для retry
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error polling SQS messages: {}", e.getMessage(), e);
        }
    }

    private void processMessage(Message message) {
        try {
            String messageBody = message.body();
            log.info("Processing SQS message: {}", message.messageId());
            
            // Переиспользуем существующую логику обработки
            orderStatusUpdateListener.handleOrderStatusUpdate(messageBody);
            
            log.info("Successfully processed SQS message: {}", message.messageId());
            
        } catch (Exception e) {
            log.error("Error processing SQS message {}: {}", message.messageId(), e.getMessage(), e);
            throw e; // Пробрасываем ошибку для retry логики
        }
    }

    private void deleteMessage(Message message) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();

            sqsClient.deleteMessage(deleteRequest);
            log.debug("Deleted SQS message: {}", message.messageId());
            
        } catch (Exception e) {
            log.error("Failed to delete SQS message {}: {}", message.messageId(), e.getMessage(), e);
        }
    }
} 
package com.ecommerce.shipping.listener;

import com.ecommerce.shipping.event.OrderStatusUpdateListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;

/**
 * Процессор для Dead Letter Queue
 * Обрабатывает проблемные сообщения и может переотправлять их в основную очередь
 */
@Service
@Profile("!test")
@Slf4j
public class DlqProcessor {

    private final OrderStatusUpdateListener orderStatusUpdateListener;
    private final SqsClient sqsClient;
    private final String dlqUrl;
    private final String mainQueueUrl;

    public DlqProcessor(
            OrderStatusUpdateListener orderStatusUpdateListener,
            SqsClient sqsClient,
            @Qualifier("orderStatusUpdateDlqUrl") String dlqUrl,
            @Qualifier("orderStatusUpdateQueueUrl") String mainQueueUrl) {
        this.orderStatusUpdateListener = orderStatusUpdateListener;
        this.sqsClient = sqsClient;
        this.dlqUrl = dlqUrl;
        this.mainQueueUrl = mainQueueUrl;
    }

    /**
     * Периодически проверяем DLQ на наличие сообщений
     * Запускается каждые 30 минут
     */
    @Scheduled(fixedDelay = 1800000) // 30 минут
    public void checkDlqMessages() {
        try {
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(dlqUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(5) // Короткий polling для DLQ
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);
            List<Message> messages = response.messages();

            if (!messages.isEmpty()) {
                log.warn("Found {} messages in DLQ! Investigating...", messages.size());
                
                for (Message message : messages) {
                    investigateFailedMessage(message);
                }
            }
        } catch (Exception e) {
            log.error("Error checking DLQ messages: {}", e.getMessage(), e);
        }
    }

    /**
     * Исследует проблемное сообщение и логирует детали
     */
    private void investigateFailedMessage(Message message) {
        try {
            String messageBody = message.body();
            String messageId = message.messageId();
            
            log.error("DLQ Message Investigation:");
            log.error("Message ID: {}", messageId);
            log.error("Message Body: {}", messageBody);
            log.error("Approximate Receive Count: {}", 
                message.attributes().get(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT));
            log.error("Sent Timestamp: {}", 
                message.attributes().get(MessageSystemAttributeName.SENT_TIMESTAMP));
            
            // Можно добавить отправку в систему мониторинга
            // sendToMonitoringSystem(messageId, messageBody);
            
        } catch (Exception e) {
            log.error("Error investigating DLQ message {}: {}", message.messageId(), e.getMessage(), e);
        }
    }

    /**
     * Ручная повторная обработка сообщения из DLQ
     * Вызывается через JMX или админ endpoint
     */
    public boolean reprocessDlqMessage(String messageId) {
        try {
            log.info("Attempting to reprocess DLQ message: {}", messageId);
            
            // Получаем все сообщения из DLQ
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(dlqUrl)
                    .maxNumberOfMessages(10)
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);
            
            for (Message message : response.messages()) {
                if (message.messageId().equals(messageId)) {
                    return attemptReprocessing(message);
                }
            }
            
            log.warn("Message {} not found in DLQ", messageId);
            return false;
            
        } catch (Exception e) {
            log.error("Error reprocessing DLQ message {}: {}", messageId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Пытается повторно обработать сообщение
     */
    private boolean attemptReprocessing(Message message) {
        try {
            String messageBody = message.body();
            String messageId = message.messageId();
            
            log.info("Attempting to reprocess message: {}", messageId);
            
            // Пытаемся обработать сообщение
            orderStatusUpdateListener.handleOrderStatusUpdate(messageBody);
            
            // Если успешно - удаляем из DLQ
            deleteDlqMessage(message);
            log.info("Successfully reprocessed and removed message {} from DLQ", messageId);
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to reprocess DLQ message {}: {}", message.messageId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Переотправляет сообщение из DLQ обратно в основную очередь
     */
    public boolean requeueDlqMessage(String messageId) {
        try {
            // Получаем сообщение из DLQ
            ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(dlqUrl)
                    .maxNumberOfMessages(10)
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);
            
            for (Message message : response.messages()) {
                if (message.messageId().equals(messageId)) {
                    return requeueMessage(message);
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error requeuing DLQ message {}: {}", messageId, e.getMessage(), e);
            return false;
        }
    }

    private boolean requeueMessage(Message message) {
        try {
            // Отправляем в основную очередь
            SendMessageRequest sendRequest = SendMessageRequest.builder()
                    .queueUrl(mainQueueUrl)
                    .messageBody(message.body())
                    .build();

            sqsClient.sendMessage(sendRequest);
            
            // Удаляем из DLQ
            deleteDlqMessage(message);
            
            log.info("Successfully requeued message {} from DLQ to main queue", message.messageId());
            return true;
            
        } catch (Exception e) {
            log.error("Failed to requeue message {}: {}", message.messageId(), e.getMessage(), e);
            return false;
        }
    }

    private void deleteDlqMessage(Message message) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(dlqUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();

            sqsClient.deleteMessage(deleteRequest);
            log.debug("Deleted DLQ message: {}", message.messageId());
            
        } catch (Exception e) {
            log.error("Failed to delete DLQ message {}: {}", message.messageId(), e.getMessage(), e);
        }
    }

    /**
     * Получает количество сообщений в DLQ
     */
    public int getDlqMessageCount() {
        try {
            GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
                    .queueUrl(dlqUrl)
                    .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                    .build();

            GetQueueAttributesResponse response = sqsClient.getQueueAttributes(request);
            return Integer.parseInt(
                response.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
            );
            
        } catch (Exception e) {
            log.error("Error getting DLQ message count: {}", e.getMessage(), e);
            return -1;
        }
    }
} 
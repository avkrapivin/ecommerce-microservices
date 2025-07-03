package com.ecommerce.lambda.service;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Реализация SnsPublisher, использующая AWS SNS клиент
 */
@Slf4j
public class AwsSnsPublisher implements SnsPublisher {
    
    private final SnsClient snsClient;
    
    public AwsSnsPublisher(SnsClient snsClient) {
        this.snsClient = snsClient;
    }
    
    @Override
    public void publishMessage(String topicArn, String message) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(message)
                    .build();
            
            snsClient.publish(request);
            log.debug("Message published to SNS topic: {}", topicArn);
        } catch (Exception e) {
            log.error("Failed to publish message to SNS topic {}: {}", topicArn, e.getMessage(), e);
            throw new RuntimeException("Failed to publish message to SNS", e);
        }
    }
} 
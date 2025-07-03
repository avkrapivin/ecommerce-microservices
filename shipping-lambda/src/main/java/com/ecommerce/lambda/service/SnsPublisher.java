package com.ecommerce.lambda.service;

/**
 * Интерфейс для публикации сообщений в SNS
 */
public interface SnsPublisher {
    
    /**
     * Публикует сообщение в указанную SNS тему
     * 
     * @param topicArn ARN темы SNS
     * @param message сообщение для публикации
     */
    void publishMessage(String topicArn, String message);
} 
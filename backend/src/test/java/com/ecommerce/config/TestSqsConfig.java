package com.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.sqs.SqsClient;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("test")
public class TestSqsConfig {

    @Bean
    @Primary
    public SqsClient sqsClient() {
        // Возвращаем мок для тестов
        return mock(SqsClient.class);
    }

    @Bean(name = "orderStatusUpdateQueueUrl")
    @Primary
    public String orderStatusUpdateQueueUrl() {
        return "https://sqs.us-east-1.amazonaws.com/123456789012/test-queue";
    }

    @Bean(name = "orderStatusUpdateDlqUrl")
    @Primary
    public String orderStatusUpdateDlqUrl() {
        return "https://sqs.us-east-1.amazonaws.com/123456789012/test-dlq";
    }
} 
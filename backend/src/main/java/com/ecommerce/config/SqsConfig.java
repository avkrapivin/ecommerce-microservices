package com.ecommerce.config;

import com.ecommerce.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

@Configuration
@Slf4j
public class SqsConfig {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.account-id}")
    private String accountId;

    @Value("${aws.sqs.order-status-update-queue-name}")
    private String orderStatusUpdateQueueName;

    @Value("${aws.sqs.order-status-update-dlq-name}")
    private String orderStatusUpdateDlqName;

    @Value("${aws.sqs.endpoint:}")
    private String sqsEndpoint;

    @Profile("!test")
    @Bean
    public SqsClient sqsClient() {
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create());
        
        if (sqsEndpoint != null && !sqsEndpoint.isEmpty()) {
            builder.endpointOverride(java.net.URI.create(sqsEndpoint));
        }
        
        return builder.build();
    }

    @Profile("!test")
    @Bean(name = "orderStatusUpdateQueueUrl")
    public String orderStatusUpdateQueueUrl(SqsClient sqsClient) {
        try {
            GetQueueUrlRequest request = GetQueueUrlRequest.builder()
                    .queueName(orderStatusUpdateQueueName)
                    .build();

            GetQueueUrlResponse response = sqsClient.getQueueUrl(request);
            String queueUrl = response.queueUrl();
            log.info("Order Status Update Queue URL: {}", queueUrl);
            return queueUrl;
        } catch (Exception e) {
            log.error("Failed to get Order Status Update queue URL", e);
            throw new ResourceNotFoundException("SQS Queue", "queueName", orderStatusUpdateQueueName);
        }
    }

    @Profile("!test")
    @Bean(name = "orderStatusUpdateDlqUrl")
    public String orderStatusUpdateDlqUrl(SqsClient sqsClient) {
        try {
            GetQueueUrlRequest request = GetQueueUrlRequest.builder()
                    .queueName(orderStatusUpdateDlqName)
                    .build();

            GetQueueUrlResponse response = sqsClient.getQueueUrl(request);
            String dlqUrl = response.queueUrl();
            log.info("Order Status Update DLQ URL: {}", dlqUrl);
            return dlqUrl;
        } catch (Exception e) {
            log.error("Failed to get Order Status Update DLQ URL", e);
            throw new ResourceNotFoundException("SQS DLQ", "queueName", orderStatusUpdateDlqName);
        }
    }
} 
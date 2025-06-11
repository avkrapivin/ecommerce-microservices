package com.ecommerce.order.event;

import com.ecommerce.order.entity.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.account-id}")
    private String accountId;

    @Value("${aws.sns.order-unconfirmed-topic-name}")
    private String orderUnconfirmedTopicName;

    public String getOrderUnconfirmedTopicArn() {
        return "arn:aws:sns:" + region + ":" + accountId + ":" + orderUnconfirmedTopicName;
    }

    public void publishOrderCreated(Order order) {
        try {
            String messageJson = objectMapper.writeValueAsString(order);
            PublishRequest request = PublishRequest.builder()
                    .topicArn(getOrderUnconfirmedTopicArn())
                    .message(messageJson)
                    .subject("OrderCreated")
                    .build();
            PublishResponse response = snsClient.publish(request);
            // Можно добавить логирование response.messageId()
        } catch (Exception e) {
            // Можно добавить логирование ошибки
        }
    }
}
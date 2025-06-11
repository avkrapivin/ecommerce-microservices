package com.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "aws")
@Getter
@Setter
public class AwsConfig {
    private String region;
    private String accountId;
    private Cognito cognito = new Cognito();

    @Getter
    @Setter
    public static class Cognito {
        private String userPoolId;
        private String clientId;
        private String clientSecret;
        private String domain;
    }

    @Bean
    public CognitoIdentityProviderClient cognitoIdentityProviderClient() {
        log.info("AWS Configuration:");
        log.info("Region: {}", region);
        log.info("User Pool ID: {}", cognito.getUserPoolId());
        log.info("Client ID: {}", cognito.getClientId());
        log.info("Domain: {}", cognito.getDomain());
        log.info("Client Secret: {}", cognito.getClientSecret() != null ? "***" : "null");

        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Profile("!test")
    @Bean
    public String orderUnconfirmedTopicArn(SnsClient snsClient) {
        try {
            GetTopicAttributesRequest request = GetTopicAttributesRequest.builder()
                    .topicArn("arn:aws:sns:" + region + ":" + accountId + ":OrderUnconfirmed")
                    .build();
            
            GetTopicAttributesResponse response = snsClient.getTopicAttributes(request);
            return response.attributes().get("TopicArn");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get SNS topic ARN", e);
        }
    }
}
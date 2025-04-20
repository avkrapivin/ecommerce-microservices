package com.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "aws")
@Getter
@Setter
public class AwsConfig {
    private String region;
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
}
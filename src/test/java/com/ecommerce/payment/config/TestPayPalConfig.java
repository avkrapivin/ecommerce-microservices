package com.ecommerce.payment.config;

import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.OAuthTokenCredential;
import com.paypal.base.rest.PayPalRESTException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class TestPayPalConfig {

    @Bean
    @Primary
    public PayPalProperties payPalProperties() {
        PayPalProperties properties = new PayPalProperties();
        PayPalProperties.Client client = new PayPalProperties.Client();
        client.setId("test-client-id");
        client.setSecret("test-client-secret");
        properties.setClient(client);
        properties.setMode("sandbox");
        return properties;
    }

    @Bean
    @Primary
    public Map<String, String> paypalSdkConfig() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("mode", "sandbox");
        return configMap;
    }

    @Bean
    @Primary
    public OAuthTokenCredential oAuthTokenCredential() {
        return new OAuthTokenCredential(
                "test-client-id",
                "test-client-secret",
                paypalSdkConfig()
        );
    }

    @Bean
    @Primary
    public APIContext apiContext() throws PayPalRESTException {
        return new APIContext(
                "test-client-id",
                "test-client-secret",
                "sandbox"
        );
    }
} 
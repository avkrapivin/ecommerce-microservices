package com.ecommerce.payment.config;

import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.OAuthTokenCredential;
import com.paypal.base.rest.PayPalRESTException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.Map;

@Profile("!test") 
@Configuration
public class PayPalConfig {

    private final PayPalProperties payPalProperties;

    public PayPalConfig(PayPalProperties payPalProperties) {
        this.payPalProperties = payPalProperties;
    }

    @Bean
    public Map<String, String> paypalSdkConfig() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("mode", payPalProperties.getMode());
        return configMap;
    }

    @Bean
    public OAuthTokenCredential oAuthTokenCredential() {
        return new OAuthTokenCredential(
                payPalProperties.getClient().getId(),
                payPalProperties.getClient().getSecret(),
                paypalSdkConfig()
        );
    }

    @Bean
    public APIContext apiContext() throws PayPalRESTException {
        APIContext context = new APIContext(
                payPalProperties.getClient().getId(),
                payPalProperties.getClient().getSecret(),
                payPalProperties.getMode()
        );
        context.setConfigurationMap(paypalSdkConfig());
        return context;
    }
} 
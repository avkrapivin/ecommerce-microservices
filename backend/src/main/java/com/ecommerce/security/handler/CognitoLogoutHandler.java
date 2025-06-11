package com.ecommerce.security.handler;

import com.ecommerce.config.AppProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CognitoLogoutHandler implements LogoutHandler {

    private final String clientId;
    private final String logoutUrl;
    private final AppProperties appProperties;
    private final RestTemplate restTemplate;

    public CognitoLogoutHandler(
            @Value("${spring.security.oauth2.client.registration.cognito.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.provider.cognito.issuer-uri}") String issuerUri,
            AppProperties appProperties) {
        this.clientId = clientId;
        this.logoutUrl = issuerUri + "/logout";
        this.appProperties = appProperties;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String logoutEndpoint = String.format("%s?client_id=%s&logout_uri=%s", 
            logoutUrl, clientId, appProperties.getLogout().getRedirectUrl());
        
        restTemplate.getForObject(logoutEndpoint, String.class);
    }
} 
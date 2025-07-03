package com.ecommerce.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public abstract class BaseLambdaTest {

    @Mock
    protected Context context;

    @Mock
    protected LambdaLogger logger;

    protected ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(context.getLogger()).thenReturn(logger);
        loadEnvironmentVariables();
    }

    protected void loadEnvironmentVariables() {
        // Load test environment variables from test.env file
        Dotenv dotenv = Dotenv.configure()
                .filename("test.env")
                .directory("src/test/resources")
                .load();
        
        // Set system properties from .env file
        dotenv.entries().forEach(entry -> 
            System.setProperty(entry.getKey(), entry.getValue())
        );
    }
} 
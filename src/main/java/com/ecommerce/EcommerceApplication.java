package com.ecommerce;

import com.ecommerce.config.AppProperties;
import com.ecommerce.config.AwsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.ecommerce.config.DotenvLoader;
import com.ecommerce.payment.config.PayPalProperties;

@SpringBootApplication(scanBasePackages = "com.ecommerce")
@EntityScan
//("com.ecommerce.user.entity")
@EnableJpaRepositories
//("com.ecommerce.user.repository")
@EnableConfigurationProperties({AwsConfig.class, AppProperties.class, PayPalProperties.class})
public class EcommerceApplication {
    public static void main(String[] args) {
        DotenvLoader.loadEnv();
        SpringApplication.run(EcommerceApplication.class, args);
    }
} 
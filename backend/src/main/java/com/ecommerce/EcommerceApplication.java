package com.ecommerce;

import com.ecommerce.config.AppProperties;
import com.ecommerce.config.AwsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.ecommerce.config.DotenvLoader;

@SpringBootApplication(scanBasePackages = "com.ecommerce")
@EntityScan
//("com.ecommerce.user.entity")
@EnableJpaRepositories
//("com.ecommerce.user.repository")
@EnableConfigurationProperties({AwsConfig.class, AppProperties.class})
@EnableScheduling
@EnableAsync
@EnableCaching
public class EcommerceApplication {
    public static void main(String[] args) {
        DotenvLoader.loadEnv();
        SpringApplication.run(EcommerceApplication.class, args);
    }
} 
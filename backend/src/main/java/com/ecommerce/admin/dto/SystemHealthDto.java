package com.ecommerce.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class SystemHealthDto {
    private String status;              // HEALTHY, WARNING, CRITICAL
    private Boolean databaseConnection;
    private String uptime;
    private String memoryUsage;
    private String cpuUsage;
    private Integer dlqMessageCount;
    private Map<String, Object> services; // Статусы различных сервисов
    private LocalDateTime lastCheck;
} 
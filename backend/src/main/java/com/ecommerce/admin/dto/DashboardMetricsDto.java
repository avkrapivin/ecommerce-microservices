package com.ecommerce.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DashboardMetricsDto {
    // Основные KPI
    private Long totalOrders;
    private Long totalUsers;
    private Long totalProducts;
    private BigDecimal totalRevenue;
    
    // Сегодняшние метрики
    private BigDecimal todayRevenue;
    private Integer ordersToday;
    private Integer newUsersToday;
    
    // Статусы
    private String systemStatus;
    private Integer dlqMessageCount;
    private Boolean databaseConnection;
    
    // Метки времени
    private LocalDateTime lastUpdated;
} 
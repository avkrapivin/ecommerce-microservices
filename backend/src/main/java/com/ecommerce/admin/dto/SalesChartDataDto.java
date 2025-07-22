package com.ecommerce.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalesChartDataDto {
    private String date;           // Дата в формате yyyy-MM-dd
    private BigDecimal revenue;    // Доход за день
    private Integer orders;        // Количество заказов
    private Integer customers;     // Количество уникальных покупателей
    private BigDecimal avgOrderValue; // Средний чек
} 
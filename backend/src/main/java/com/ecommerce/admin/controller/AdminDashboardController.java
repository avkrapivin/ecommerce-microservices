package com.ecommerce.admin.controller;

import com.ecommerce.admin.dto.*;
import com.ecommerce.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin контроллер для дашборда и метрик
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Profile("!test")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * Получить основные метрики для дашборда
     */
    @GetMapping("/metrics/summary")
    public ResponseEntity<DashboardMetricsDto> getDashboardMetrics() {
        try {
            DashboardMetricsDto metrics = adminDashboardService.getDashboardMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting dashboard metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Получить данные для графика продаж
     */
    @GetMapping("/metrics/sales-chart")
    public ResponseEntity<List<SalesChartDataDto>> getSalesChartData(
            @RequestParam(required = false, defaultValue = "30") int days) {
        try {
            List<SalesChartDataDto> salesData = adminDashboardService.getSalesChartData(days);
            return ResponseEntity.ok(salesData);
        } catch (Exception e) {
            log.error("Error getting sales chart data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Получить топ продукты
     */
    @GetMapping("/products/top-selling")
    public ResponseEntity<List<TopProductDto>> getTopSellingProducts(
            @RequestParam(required = false, defaultValue = "10") int limit) {
        try {
            List<TopProductDto> products = adminDashboardService.getTopSellingProducts(limit);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error getting top selling products: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 
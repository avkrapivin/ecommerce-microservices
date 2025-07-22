package com.ecommerce.admin.controller;

import com.ecommerce.admin.dto.SystemHealthDto;
import com.ecommerce.admin.service.AdminSystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin контроллер для системного мониторинга
 */
@RestController
@RequestMapping("/admin/system")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Profile("!test")
public class AdminSystemController {

    private final AdminSystemService adminSystemService;

    /**
     * Получить общее состояние системы
     */
    @GetMapping("/health")
    public ResponseEntity<SystemHealthDto> getSystemHealth() {
        try {
            SystemHealthDto health = adminSystemService.getSystemHealth();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error getting system health: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Получить метрики производительности
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        try {
            Map<String, Object> metrics = adminSystemService.getPerformanceMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("Error getting performance metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }



    /**
     * Очистить кэш системы
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        try {
            adminSystemService.clearSystemCache();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "System cache cleared successfully"
            ));
        } catch (Exception e) {
            log.error("Error clearing cache: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "Failed to clear cache: " + e.getMessage()
            ));
        }
    }
} 
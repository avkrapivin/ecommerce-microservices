package com.ecommerce.admin.service;

import com.ecommerce.admin.dto.SystemHealthDto;
import com.ecommerce.shipping.listener.DlqProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class AdminSystemService {

    private final DlqProcessor dlqProcessor;
    private final DataSource dataSource;
    private final CacheManager cacheManager;

    public SystemHealthDto getSystemHealth() {
        SystemHealthDto health = new SystemHealthDto();
        
        // Проверка статуса системы
        health.setStatus(determineSystemStatus());
        health.setDatabaseConnection(checkDatabaseConnection());
        health.setUptime(getSystemUptime());
        health.setMemoryUsage(getMemoryUsage());
        health.setCpuUsage(getCpuUsage());
        health.setDlqMessageCount(dlqProcessor.getDlqMessageCount());
        health.setLastCheck(LocalDateTime.now());
        
        // Статусы сервисов
        health.setServices(Map.of(
            "database", health.getDatabaseConnection(),
            "dlq", health.getDlqMessageCount() == 0,
            "cache", cacheManager != null
        ));
        
        return health;
    }

    public Map<String, Object> getPerformanceMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return Map.of(
            "memory", Map.of(
                "total", totalMemory / 1024 / 1024 + " MB",
                "used", usedMemory / 1024 / 1024 + " MB",
                "free", freeMemory / 1024 / 1024 + " MB",
                "usage", Math.round((double) usedMemory / totalMemory * 100) + "%"
            ),
            "threads", Map.of(
                "active", Thread.activeCount()
            ),
            "uptime", getSystemUptime()
        );
    }



    public void clearSystemCache() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    log.info("Cleared cache: {}", cacheName);
                }
            });
        }
    }

    private String determineSystemStatus() {
        boolean dbOk = checkDatabaseConnection();
        int dlqCount = dlqProcessor.getDlqMessageCount();
        
        if (!dbOk) return "CRITICAL";
        if (dlqCount > 10) return "WARNING";
        return "HEALTHY";
    }

    private boolean checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5 секунд таймаут
        } catch (Exception e) {
            log.error("Database connection check failed: {}", e.getMessage());
            return false;
        }
    }

    private String getSystemUptime() {
        long uptime = System.currentTimeMillis() - getJvmStartTime();
        long hours = uptime / (1000 * 60 * 60);
        long minutes = (uptime % (1000 * 60 * 60)) / (1000 * 60);
        return String.format("%dh %dm", hours, minutes);
    }

    private String getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        return Math.round((double) usedMemory / totalMemory * 100) + "%";
    }

    private String getCpuUsage() {
        try {
            // Попытка получить CPU usage через JMX
            java.lang.management.OperatingSystemMXBean osBean = 
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                double cpuUsage = sunOsBean.getProcessCpuLoad() * 100;
                if (cpuUsage >= 0) {
                    return String.format("%.1f%%", cpuUsage);
                }
            }
            
            // Fallback если JMX недоступен
            return "N/A";
        } catch (Exception e) {
            log.warn("Unable to get CPU usage: {}", e.getMessage());
            return "N/A";
        }
    }

    private long getJvmStartTime() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
    }
} 
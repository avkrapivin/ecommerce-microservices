package com.ecommerce.admin.service;

import com.ecommerce.admin.dto.SystemHealthDto;
import com.ecommerce.shipping.listener.DlqProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminSystemServiceTest {

    @Mock
    private DlqProcessor dlqProcessor;

    @Mock
    private DataSource dataSource;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Connection connection;

    @Mock
    private Cache cache;

    @InjectMocks
    private AdminSystemService adminSystemService;

    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(5)).thenReturn(true);
        when(dlqProcessor.getDlqMessageCount()).thenReturn(0);
    }

    @Test
    void getSystemHealth_WhenAllSystemsHealthy_ShouldReturnHealthyStatus() {
        // When
        SystemHealthDto health = adminSystemService.getSystemHealth();

        // Then
        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isEqualTo("HEALTHY");
        assertThat(health.getDatabaseConnection()).isTrue();
        assertThat(health.getDlqMessageCount()).isEqualTo(0);
        assertThat(health.getUptime()).isNotNull();
        assertThat(health.getMemoryUsage()).isNotNull();
        assertThat(health.getCpuUsage()).isNotNull();
        assertThat(health.getLastCheck()).isNotNull();
        assertThat(health.getServices()).isNotNull();
        assertThat(health.getServices()).containsKeys("database", "dlq", "cache");
    }

    @Test
    void getSystemHealth_WhenDatabaseDown_ShouldReturnCriticalStatus() throws SQLException {
        // Given
        when(connection.isValid(5)).thenReturn(false);

        // When
        SystemHealthDto health = adminSystemService.getSystemHealth();

        // Then
        assertThat(health.getStatus()).isEqualTo("CRITICAL");
        assertThat(health.getDatabaseConnection()).isFalse();
    }

    @Test
    void getSystemHealth_WhenDatabaseConnectionFails_ShouldReturnCriticalStatus() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // When
        SystemHealthDto health = adminSystemService.getSystemHealth();

        // Then
        assertThat(health.getStatus()).isEqualTo("CRITICAL");
        assertThat(health.getDatabaseConnection()).isFalse();
    }

    @Test
    void getSystemHealth_WhenManyDlqMessages_ShouldReturnWarningStatus() {
        // Given
        when(dlqProcessor.getDlqMessageCount()).thenReturn(15);

        // When
        SystemHealthDto health = adminSystemService.getSystemHealth();

        // Then
        assertThat(health.getStatus()).isEqualTo("WARNING");
        assertThat(health.getDlqMessageCount()).isEqualTo(15);
    }

    @Test
    void getPerformanceMetrics_ShouldReturnValidMetrics() {
        // When
        Map<String, Object> metrics = adminSystemService.getPerformanceMetrics();

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics).containsKeys("memory", "threads", "uptime");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> memoryInfo = (Map<String, Object>) metrics.get("memory");
        assertThat(memoryInfo).containsKeys("total", "used", "free", "usage");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> threadInfo = (Map<String, Object>) metrics.get("threads");
        assertThat(threadInfo).containsKey("active");
        
        assertThat(metrics.get("uptime")).isNotNull();
    }

    @Test
    void clearSystemCache_WhenCacheManagerExists_ShouldClearAllCaches() {
        // Given
        Set<String> cacheNames = Set.of("cache1", "cache2");
        when(cacheManager.getCacheNames()).thenReturn(cacheNames);
        when(cacheManager.getCache("cache1")).thenReturn(cache);
        when(cacheManager.getCache("cache2")).thenReturn(cache);

        // When
        adminSystemService.clearSystemCache();

        // Then
        verify(cache, times(2)).clear();
    }

    @Test
    void clearSystemCache_WhenCacheManagerIsNull_ShouldNotThrowException() {
        // Given
        adminSystemService = new AdminSystemService(dlqProcessor, dataSource, null);

        // When & Then - should not throw exception
        adminSystemService.clearSystemCache();
    }

    @Test
    void clearSystemCache_WhenCacheIsNull_ShouldNotThrowException() {
        // Given
        Set<String> cacheNames = Set.of("cache1");
        when(cacheManager.getCacheNames()).thenReturn(cacheNames);
        when(cacheManager.getCache("cache1")).thenReturn(null);

        // When & Then - should not throw exception
        adminSystemService.clearSystemCache();
    }
} 
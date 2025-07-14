package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.ExceptionEvent;
import com.ecommerce.lambda.model.ExceptionLevel;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для публикации метрик ошибок в CloudWatch
 */
@Slf4j
public class MetricsPublisher {
    
    private final CloudWatchClient cloudWatchClient;
    private final String namespace;
    
    public MetricsPublisher(CloudWatchClient cloudWatchClient, String namespace) {
        this.cloudWatchClient = cloudWatchClient;
        this.namespace = namespace;
    }
    
    /**
     * Публикует метрики для события ошибки
     */
    public void publishExceptionMetrics(ExceptionEvent event) {
        if (event == null) {
            log.warn("Cannot publish metrics for null event");
            return;
        }
        
        try {
            List<MetricDatum> metrics = createMetrics(event);
            
            if (!metrics.isEmpty()) {
                PutMetricDataRequest request = PutMetricDataRequest.builder()
                        .namespace(namespace)
                        .metricData(metrics)
                        .build();
                
                cloudWatchClient.putMetricData(request);
                log.debug("Published {} metrics for exception: {}", metrics.size(), event.getExceptionType());
            }
            
        } catch (Exception e) {
            log.error("Failed to publish metrics for exception: {}", event.getExceptionType(), e);
            // Не прерываем обработку из-за ошибки метрик
        }
    }
    
    /**
     * Публикует метрики для группированной ошибки
     */
    public void publishGroupedExceptionMetrics(ExceptionEvent event, long count) {
        try {
            List<MetricDatum> metrics = createGroupedMetrics(event, count);
            
            if (!metrics.isEmpty()) {
                PutMetricDataRequest request = PutMetricDataRequest.builder()
                        .namespace(namespace)
                        .metricData(metrics)
                        .build();
                
                cloudWatchClient.putMetricData(request);
                log.debug("Published {} grouped metrics for exception: {}", metrics.size(), event.getExceptionType());
            }
            
        } catch (Exception e) {
            log.error("Failed to publish grouped metrics for exception: {}", event.getExceptionType(), e);
        }
    }
    
    /**
     * Создает метрики для нового события ошибки
     */
    private List<MetricDatum> createMetrics(ExceptionEvent event) {
        List<MetricDatum> metrics = new ArrayList<>();
        Instant timestamp = event.getTimestamp();
        
        // Общий счетчик ошибок
        metrics.add(MetricDatum.builder()
                .metricName("ExceptionCount")
                .value(1.0)
                .unit(StandardUnit.COUNT)
                .timestamp(timestamp)
                .dimensions(
                        Dimension.builder().name("Service").value(event.getService()).build(),
                        Dimension.builder().name("Environment").value(event.getEnvironment()).build()
                )
                .build());
        
        // Счетчик по типу ошибки
        metrics.add(MetricDatum.builder()
                .metricName("ExceptionCountByType")
                .value(1.0)
                .unit(StandardUnit.COUNT)
                .timestamp(timestamp)
                .dimensions(
                        Dimension.builder().name("Service").value(event.getService()).build(),
                        Dimension.builder().name("ExceptionType").value(event.getExceptionType()).build(),
                        Dimension.builder().name("Environment").value(event.getEnvironment()).build()
                )
                .build());
        
        // Счетчик по уровню критичности
        metrics.add(MetricDatum.builder()
                .metricName("ExceptionCountByLevel")
                .value(1.0)
                .unit(StandardUnit.COUNT)
                .timestamp(timestamp)
                .dimensions(
                        Dimension.builder().name("Service").value(event.getService()).build(),
                        Dimension.builder().name("Level").value(event.getLevel().toString()).build(),
                        Dimension.builder().name("Environment").value(event.getEnvironment()).build()
                )
                .build());
        
        // Дополнительные метрики для критических ошибок
        if (event.getLevel().isCritical()) {
            metrics.add(MetricDatum.builder()
                    .metricName("CriticalExceptionCount")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .timestamp(timestamp)
                    .dimensions(
                            Dimension.builder().name("Service").value(event.getService()).build(),
                            Dimension.builder().name("Environment").value(event.getEnvironment()).build()
                    )
                    .build());
        }
        
        // Метрики для FATAL ошибок
        if (event.getLevel() == ExceptionLevel.FATAL) {
            metrics.add(MetricDatum.builder()
                    .metricName("FatalExceptionCount")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .timestamp(timestamp)
                    .dimensions(
                            Dimension.builder().name("Service").value(event.getService()).build(),
                            Dimension.builder().name("Environment").value(event.getEnvironment()).build()
                    )
                    .build());
        }
        
        // Метрики по версии сервиса (если указана)
        if (event.getVersion() != null) {
            metrics.add(MetricDatum.builder()
                    .metricName("ExceptionCountByVersion")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .timestamp(timestamp)
                    .dimensions(
                            Dimension.builder().name("Service").value(event.getService()).build(),
                            Dimension.builder().name("Version").value(event.getVersion()).build(),
                            Dimension.builder().name("Environment").value(event.getEnvironment()).build()
                    )
                    .build());
        }
        
        // Метрики по тегам (если указаны)
        if (event.getTags() != null && !event.getTags().isEmpty()) {
            for (String tag : event.getTags()) {
                metrics.add(MetricDatum.builder()
                        .metricName("ExceptionCountByTag")
                        .value(1.0)
                        .unit(StandardUnit.COUNT)
                        .timestamp(timestamp)
                        .dimensions(
                                Dimension.builder().name("Service").value(event.getService()).build(),
                                Dimension.builder().name("Tag").value(tag).build(),
                                Dimension.builder().name("Environment").value(event.getEnvironment()).build()
                        )
                        .build());
            }
        }
        
        return metrics;
    }
    
    /**
     * Создает метрики для группированной ошибки
     */
    private List<MetricDatum> createGroupedMetrics(ExceptionEvent event, long count) {
        List<MetricDatum> metrics = new ArrayList<>();
        Instant timestamp = event.getTimestamp();
        
        // Метрика частоты ошибок
        metrics.add(MetricDatum.builder()
                .metricName("ExceptionFrequency")
                .value((double) count)
                .unit(StandardUnit.COUNT)
                .timestamp(timestamp)
                .dimensions(
                        Dimension.builder().name("Service").value(event.getService()).build(),
                        Dimension.builder().name("ExceptionType").value(event.getExceptionType()).build(),
                        Dimension.builder().name("Environment").value(event.getEnvironment()).build()
                )
                .build());
        
        // Метрика для алертов при превышении порога
        if (count > 10) { // Порог для алерта
            metrics.add(MetricDatum.builder()
                    .metricName("HighFrequencyException")
                    .value(1.0)
                    .unit(StandardUnit.COUNT)
                    .timestamp(timestamp)
                    .dimensions(
                            Dimension.builder().name("Service").value(event.getService()).build(),
                            Dimension.builder().name("ExceptionType").value(event.getExceptionType()).build(),
                            Dimension.builder().name("Environment").value(event.getEnvironment()).build()
                    )
                    .build());
        }
        
        return metrics;
    }
} 
package com.ecommerce.lambda.service;

import com.ecommerce.lambda.model.ExceptionEvent;
import com.ecommerce.lambda.model.ExceptionLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsPublisherTest {

    @Mock
    private CloudWatchClient cloudWatchClient;

    private MetricsPublisher metricsPublisher;
    private final String namespace = "ECommerce/Test/Exceptions";

    @BeforeEach
    void setUp() {
        metricsPublisher = new MetricsPublisher(cloudWatchClient, namespace);
    }

    @Test
    void shouldPublishExceptionMetricsSuccessfully() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
                .thenReturn(PutMetricDataResponse.builder().build());

        // When
        metricsPublisher.publishExceptionMetrics(event);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(cloudWatchClient).putMetricData(captor.capture());
        
        PutMetricDataRequest request = captor.getValue();
        assertThat(request.namespace()).isEqualTo(namespace);
        assertThat(request.metricData()).isNotEmpty();
        
        // Проверяем базовые метрики
        List<MetricDatum> metrics = request.metricData();
        assertThat(metrics).anyMatch(m -> m.metricName().equals("ExceptionCount"));
        assertThat(metrics).anyMatch(m -> m.metricName().equals("ExceptionCountByType"));
        assertThat(metrics).anyMatch(m -> m.metricName().equals("ExceptionCountByLevel"));
        assertThat(metrics).anyMatch(m -> m.metricName().equals("CriticalExceptionCount"));
    }

    @Test
    void shouldPublishFatalExceptionMetrics() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.FATAL);
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
                .thenReturn(PutMetricDataResponse.builder().build());

        // When
        metricsPublisher.publishExceptionMetrics(event);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(cloudWatchClient).putMetricData(captor.capture());
        
        List<MetricDatum> metrics = captor.getValue().metricData();
        assertThat(metrics).anyMatch(m -> m.metricName().equals("FatalExceptionCount"));
        assertThat(metrics).anyMatch(m -> m.metricName().equals("CriticalExceptionCount"));
    }

    @Test
    void shouldNotPublishCriticalMetricsForWarnings() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.WARN);
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
                .thenReturn(PutMetricDataResponse.builder().build());

        // When
        metricsPublisher.publishExceptionMetrics(event);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(cloudWatchClient).putMetricData(captor.capture());
        
        List<MetricDatum> metrics = captor.getValue().metricData();
        assertThat(metrics).noneMatch(m -> m.metricName().equals("CriticalExceptionCount"));
        assertThat(metrics).noneMatch(m -> m.metricName().equals("FatalExceptionCount"));
    }

    @Test
    void shouldPublishMetricsWithCorrectDimensions() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
                .thenReturn(PutMetricDataResponse.builder().build());

        // When
        metricsPublisher.publishExceptionMetrics(event);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(cloudWatchClient).putMetricData(captor.capture());
        
        List<MetricDatum> metrics = captor.getValue().metricData();
        
        // Проверяем ExceptionCount метрику
        MetricDatum exceptionCountMetric = metrics.stream()
                .filter(m -> m.metricName().equals("ExceptionCount"))
                .findFirst()
                .orElseThrow();
        
        assertThat(exceptionCountMetric.dimensions()).hasSize(2);
        assertThat(exceptionCountMetric.dimensions()).anyMatch(d -> 
                d.name().equals("Service") && d.value().equals("test-service"));
        assertThat(exceptionCountMetric.dimensions()).anyMatch(d -> 
                d.name().equals("Environment") && d.value().equals("test"));
    }

    @Test
    void shouldPublishVersionMetricsWhenVersionProvided() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        event.setVersion("1.2.3");
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
                .thenReturn(PutMetricDataResponse.builder().build());

        // When
        metricsPublisher.publishExceptionMetrics(event);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(cloudWatchClient).putMetricData(captor.capture());
        
        List<MetricDatum> metrics = captor.getValue().metricData();
        assertThat(metrics).anyMatch(m -> m.metricName().equals("ExceptionCountByVersion"));
        
        MetricDatum versionMetric = metrics.stream()
                .filter(m -> m.metricName().equals("ExceptionCountByVersion"))
                .findFirst()
                .orElseThrow();
        
        assertThat(versionMetric.dimensions()).anyMatch(d -> 
                d.name().equals("Version") && d.value().equals("1.2.3"));
    }

    @Test
    void shouldPublishTagMetricsWhenTagsProvided() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        event.setTags(List.of("payment", "critical-path"));
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
                .thenReturn(PutMetricDataResponse.builder().build());

        // When
        metricsPublisher.publishExceptionMetrics(event);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(cloudWatchClient).putMetricData(captor.capture());
        
        List<MetricDatum> metrics = captor.getValue().metricData();
        List<MetricDatum> tagMetrics = metrics.stream()
                .filter(m -> m.metricName().equals("ExceptionCountByTag"))
                .toList();
        
        assertThat(tagMetrics).hasSize(2);
        assertThat(tagMetrics).anyMatch(m -> 
                m.dimensions().stream().anyMatch(d -> d.name().equals("Tag") && d.value().equals("payment")));
        assertThat(tagMetrics).anyMatch(m -> 
                m.dimensions().stream().anyMatch(d -> d.name().equals("Tag") && d.value().equals("critical-path")));
    }

    @Test
    void shouldPublishGroupedExceptionMetrics() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        long count = 5L;
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
                .thenReturn(PutMetricDataResponse.builder().build());

        // When
        metricsPublisher.publishGroupedExceptionMetrics(event, count);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(cloudWatchClient).putMetricData(captor.capture());
        
        List<MetricDatum> metrics = captor.getValue().metricData();
        assertThat(metrics).anyMatch(m -> m.metricName().equals("ExceptionFrequency"));
        
        MetricDatum frequencyMetric = metrics.stream()
                .filter(m -> m.metricName().equals("ExceptionFrequency"))
                .findFirst()
                .orElseThrow();
        
        assertThat(frequencyMetric.value()).isEqualTo(5.0);
    }

    @Test
    void shouldPublishHighFrequencyMetricWhenCountExceedsThreshold() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        long count = 15L; // Превышает порог 10
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
                .thenReturn(PutMetricDataResponse.builder().build());

        // When
        metricsPublisher.publishGroupedExceptionMetrics(event, count);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(cloudWatchClient).putMetricData(captor.capture());
        
        List<MetricDatum> metrics = captor.getValue().metricData();
        assertThat(metrics).anyMatch(m -> m.metricName().equals("HighFrequencyException"));
    }

    @Test
    void shouldNotPublishHighFrequencyMetricWhenCountBelowThreshold() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        long count = 5L; // Ниже порога 10
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
                .thenReturn(PutMetricDataResponse.builder().build());

        // When
        metricsPublisher.publishGroupedExceptionMetrics(event, count);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(cloudWatchClient).putMetricData(captor.capture());
        
        List<MetricDatum> metrics = captor.getValue().metricData();
        assertThat(metrics).noneMatch(m -> m.metricName().equals("HighFrequencyException"));
    }

    @Test
    void shouldHandleCloudWatchExceptionGracefully() {
        // Given
        ExceptionEvent event = createTestEvent(ExceptionLevel.ERROR);
        when(cloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
                .thenThrow(CloudWatchException.builder().message("CloudWatch error").build());

        // When & Then (не должно выбрасывать исключение)
        metricsPublisher.publishExceptionMetrics(event);
        
        verify(cloudWatchClient).putMetricData(any(PutMetricDataRequest.class));
    }

    @Test
    void shouldNotPublishWhenNoMetrics() {
        // Given
        ExceptionEvent event = null;

        // When
        metricsPublisher.publishExceptionMetrics(event);

        // Then
        verify(cloudWatchClient, never()).putMetricData(any(PutMetricDataRequest.class));
    }

    private ExceptionEvent createTestEvent(ExceptionLevel level) {
        return ExceptionEvent.builder()
                .service("test-service")
                .timestamp(Instant.parse("2024-01-01T12:00:00Z"))
                .exceptionType("NullPointerException")
                .message("Test exception message")
                .level(level)
                .environment("test")
                .build();
    }
} 
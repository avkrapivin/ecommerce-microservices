package com.ecommerce.admin.service;

import com.ecommerce.admin.dto.*;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OrderItemRepository;
import com.ecommerce.products.repository.ProductRepository;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.shipping.listener.DlqProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminDashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DlqProcessor dlqProcessor;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @BeforeEach
    void setUp() {
        // Мокаем данные для тестов
        when(orderRepository.count()).thenReturn(100L);
        when(userRepository.count()).thenReturn(50L);
        when(productRepository.count()).thenReturn(25L);
        when(orderRepository.calculateTotalRevenue()).thenReturn(BigDecimal.valueOf(50000));
        when(orderRepository.calculateTodayRevenue(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.valueOf(2500));
        when(orderRepository.countTodayOrders(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(15);
        when(userRepository.countTodayUsers(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3);
        when(dlqProcessor.getDlqMessageCount()).thenReturn(0);
    }

    @Test
    void getDashboardMetrics_ShouldReturnValidMetrics() {
        // When
        DashboardMetricsDto metrics = adminDashboardService.getDashboardMetrics();

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalOrders()).isEqualTo(100L);
        assertThat(metrics.getTotalUsers()).isEqualTo(50L);
        assertThat(metrics.getTotalProducts()).isEqualTo(25L);
        assertThat(metrics.getTotalRevenue()).isEqualTo(BigDecimal.valueOf(50000));
        assertThat(metrics.getTodayRevenue()).isEqualTo(BigDecimal.valueOf(2500));
        assertThat(metrics.getOrdersToday()).isEqualTo(15);
        assertThat(metrics.getNewUsersToday()).isEqualTo(3);
        assertThat(metrics.getSystemStatus()).isEqualTo("HEALTHY");
        assertThat(metrics.getDlqMessageCount()).isEqualTo(0);
        assertThat(metrics.getDatabaseConnection()).isTrue();
        assertThat(metrics.getLastUpdated()).isNotNull();
    }

    @Test
    void getSalesChartData_ShouldReturnValidChartData() {
        // Given
        List<Object[]> salesData = new ArrayList<>();
        salesData.add(new Object[]{"2025-01-20", BigDecimal.valueOf(1000), 10L, 8L});
        salesData.add(new Object[]{"2025-01-21", BigDecimal.valueOf(1500), 15L, 12L});
        when(orderRepository.getSalesDataByDateRange(any(LocalDateTime.class)))
                .thenReturn(salesData);

        // When
        List<SalesChartDataDto> chartData = adminDashboardService.getSalesChartData(7);

        // Then
        assertThat(chartData).isNotEmpty();
        assertThat(chartData).hasSize(2);
        
        SalesChartDataDto firstDay = chartData.get(0);
        assertThat(firstDay.getDate()).isEqualTo("2025-01-20");
        assertThat(firstDay.getRevenue()).isEqualTo(BigDecimal.valueOf(1000));
        assertThat(firstDay.getOrders()).isEqualTo(10);
        assertThat(firstDay.getCustomers()).isEqualTo(8);
        assertThat(firstDay.getAvgOrderValue()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
    }

    @Test
    void getSalesChartData_WithZeroOrders_ShouldHandleGracefully() {
        // Given
        List<Object[]> emptyData = new ArrayList<>();
        emptyData.add(new Object[]{"2025-01-20", BigDecimal.ZERO, 0L, 0L});
        when(orderRepository.getSalesDataByDateRange(any(LocalDateTime.class)))
                .thenReturn(emptyData);

        // When
        List<SalesChartDataDto> chartData = adminDashboardService.getSalesChartData(1);

        // Then
        assertThat(chartData).isNotEmpty();
        SalesChartDataDto dayData = chartData.get(0);
        assertThat(dayData.getAvgOrderValue()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getTopSellingProducts_ShouldReturnValidProductData() {
        // Given
        List<Object[]> productData = new ArrayList<>();
        productData.add(new Object[]{1L, "Product 1", "SKU001", 150L, BigDecimal.valueOf(15000), "Electronics", "image1.jpg", BigDecimal.valueOf(4.5)});
        productData.add(new Object[]{2L, "Product 2", "SKU002", 120L, BigDecimal.valueOf(12000), "Books", "image2.jpg", BigDecimal.valueOf(4.2)});
        when(orderItemRepository.findTopSellingProducts())
                .thenReturn(productData);

        // When
        List<TopProductDto> topProducts = adminDashboardService.getTopSellingProducts(5);

        // Then
        assertThat(topProducts).isNotEmpty();
        assertThat(topProducts).hasSize(2);
        
        TopProductDto firstProduct = topProducts.get(0);
        assertThat(firstProduct.getProductId()).isEqualTo(1L);
        assertThat(firstProduct.getProductName()).isEqualTo("Product 1");
        assertThat(firstProduct.getSku()).isEqualTo("SKU001");
        assertThat(firstProduct.getUnitsSold()).isEqualTo(150);
        assertThat(firstProduct.getRevenue()).isEqualTo(BigDecimal.valueOf(15000));
        assertThat(firstProduct.getCategoryName()).isEqualTo("Electronics");
        assertThat(firstProduct.getImageUrl()).isEqualTo("image1.jpg");
        assertThat(firstProduct.getAverageRating()).isEqualTo(BigDecimal.valueOf(4.5));
    }

    @Test
    void getTopSellingProducts_WithNullImageAndRating_ShouldUseDefaults() {
        // Given
        List<Object[]> nullData = new ArrayList<>();
        nullData.add(new Object[]{1L, "Product 1", "SKU001", 150L, BigDecimal.valueOf(15000), "Electronics", null, null});
        when(orderItemRepository.findTopSellingProducts())
                .thenReturn(nullData);

        // When
        List<TopProductDto> topProducts = adminDashboardService.getTopSellingProducts(1);

        // Then
        assertThat(topProducts).isNotEmpty();
        TopProductDto product = topProducts.get(0);
        assertThat(product.getImageUrl()).isEqualTo("default-product.jpg");
        assertThat(product.getAverageRating()).isEqualTo(BigDecimal.ZERO);
    }
} 
package com.ecommerce.admin.service;

import com.ecommerce.admin.dto.*;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.repository.OrderItemRepository;
import com.ecommerce.products.repository.ProductRepository;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.shipping.listener.DlqProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DlqProcessor dlqProcessor;

    public DashboardMetricsDto getDashboardMetrics() {
        DashboardMetricsDto metrics = new DashboardMetricsDto();
        
        // Основные KPI
        metrics.setTotalOrders(orderRepository.count());
        metrics.setTotalUsers(userRepository.count());
        metrics.setTotalProducts(productRepository.count());
        metrics.setTotalRevenue(orderRepository.calculateTotalRevenue());
        
        // Сегодняшние метрики  
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = todayStart.plusDays(1);
        
        metrics.setTodayRevenue(orderRepository.calculateTodayRevenue(todayStart, todayEnd));
        metrics.setOrdersToday(orderRepository.countTodayOrders(todayStart, todayEnd));
        metrics.setNewUsersToday(userRepository.countTodayUsers(todayStart, todayEnd));
        
        // Статусы системы
        metrics.setSystemStatus("HEALTHY");
        metrics.setDlqMessageCount(dlqProcessor.getDlqMessageCount());
        metrics.setDatabaseConnection(true);
        metrics.setLastUpdated(LocalDateTime.now());
        
        return metrics;
    }

    public List<SalesChartDataDto> getSalesChartData(int days) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime startDate = LocalDateTime.now().minusDays(days).withHour(0).withMinute(0).withSecond(0);
        
        // Получаем реальные данные из БД
        List<Object[]> salesData = orderRepository.getSalesDataByDateRange(startDate);
        
        return salesData.stream()
            .map(row -> {
                SalesChartDataDto data = new SalesChartDataDto();
                data.setDate(row[0].toString()); // orderDate
                data.setRevenue((BigDecimal) row[1]); // revenue
                data.setOrders(((Number) row[2]).intValue()); // orderCount
                data.setCustomers(((Number) row[3]).intValue()); // customerCount
                
                if (data.getOrders() > 0) {
                    data.setAvgOrderValue(data.getRevenue().divide(
                        BigDecimal.valueOf(data.getOrders()), 
                        2, 
                        java.math.RoundingMode.HALF_UP
                    ));
                } else {
                    data.setAvgOrderValue(BigDecimal.ZERO);
                }
                
                return data;
            })
            .toList();
    }

    public List<TopProductDto> getTopSellingProducts(int limit) {
        // Получаем реальные данные из БД
        List<Object[]> topProducts = orderItemRepository.findTopSellingProducts();
        
        return topProducts.stream()
            .limit(limit)
            .map(row -> {
                TopProductDto product = new TopProductDto();
                product.setProductId(((Number) row[0]).longValue()); // product.id
                product.setProductName((String) row[1]); // product.name
                product.setSku((String) row[2]); // product.sku
                product.setUnitsSold(((Number) row[3]).intValue()); // unitsSold
                product.setRevenue((BigDecimal) row[4]); // revenue
                product.setCategoryName((String) row[5]); // category.name
                product.setImageUrl(row[6] != null ? (String) row[6] : "default-product.jpg"); // imageUrl
                product.setAverageRating(row[7] != null ? (BigDecimal) row[7] : BigDecimal.ZERO); // averageRating
                
                return product;
            })
            .toList();
    }


} 
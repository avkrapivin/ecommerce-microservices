package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentStatus;
import com.ecommerce.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.status = :status")
    List<Order> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.paymentStatus = :paymentStatus")
    List<Order> findByPaymentStatus(@Param("paymentStatus") PaymentStatus paymentStatus);
    
    @Query("SELECT o FROM Order o WHERE o.status = :status")
    List<Order> findByStatus(@Param("status") OrderStatus status);
    
    Optional<Order> findByPaymentId(String paymentId);
    
    // Admin analytics queries
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED')")
    BigDecimal calculateTotalRevenue();
    
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED') AND o.createdAt >= :todayStart AND o.createdAt < :todayEnd")
    BigDecimal calculateTodayRevenue(@Param("todayStart") LocalDateTime todayStart, @Param("todayEnd") LocalDateTime todayEnd);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :todayStart AND o.createdAt < :todayEnd")
    Integer countTodayOrders(@Param("todayStart") LocalDateTime todayStart, @Param("todayEnd") LocalDateTime todayEnd);
    
    @Query("SELECT FUNCTION('DATE', o.createdAt) as orderDate, COALESCE(SUM(o.total), 0) as revenue, COUNT(o) as orderCount, COUNT(DISTINCT o.user.id) as customerCount " +
           "FROM Order o WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED') AND o.createdAt >= :startDate " +
           "GROUP BY FUNCTION('DATE', o.createdAt) ORDER BY FUNCTION('DATE', o.createdAt)")
    List<Object[]> getSalesDataByDateRange(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findAllOrderByCreatedAtDesc();
} 
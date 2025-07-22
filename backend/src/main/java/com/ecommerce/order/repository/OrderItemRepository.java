package com.ecommerce.order.repository;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder(Order order);
    
    List<OrderItem> findByProduct(Product product);
    
    @Modifying
    @Transactional
    void deleteByOrder(Order order);
    
    // Admin analytics queries
    @Query("SELECT oi.product.id, oi.product.name, oi.product.sku, SUM(oi.quantity) as unitsSold, " +
           "SUM(oi.totalPrice) as revenue, oi.product.category.name, " +
           "(SELECT pi.imageUrl FROM ProductImage pi WHERE pi.product.id = oi.product.id ORDER BY pi.id LIMIT 1), " +
           "(SELECT COALESCE(AVG(pr.rating), 0) FROM ProductReview pr WHERE pr.product.id = oi.product.id) " +
           "FROM OrderItem oi WHERE oi.order.status IN ('PAID', 'SHIPPED', 'DELIVERED') " +
           "GROUP BY oi.product.id, oi.product.name, oi.product.sku, oi.product.category.name " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingProducts();
} 
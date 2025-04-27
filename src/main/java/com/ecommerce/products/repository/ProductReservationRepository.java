package com.ecommerce.products.repository;

import com.ecommerce.products.entity.Product;
import com.ecommerce.products.entity.ProductReservation;
import com.ecommerce.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReservationRepository extends JpaRepository<ProductReservation, Long> {
    Optional<ProductReservation> findByProductIdAndUserIdAndActiveTrue(Long productId, Long userId);
    
    List<ProductReservation> findByProductIdAndActiveTrue(Long productId);
    
    List<ProductReservation> findByUserIdAndActiveTrue(Long userId);
    
    List<ProductReservation> findByExpiresAtBeforeAndActiveTrue(LocalDateTime dateTime);
    
    @Modifying
    @Transactional
    @Query("UPDATE ProductReservation pr SET pr.active = false WHERE pr.expiresAt < ?1 AND pr.active = true")
    int deactivateExpiredReservations(LocalDateTime dateTime);
    
    @Modifying
    @Transactional
    @Query("UPDATE ProductReservation pr SET pr.active = false WHERE pr.product = ?1 AND pr.user = ?2 AND pr.active = true")
    int deactivateReservationsForUser(Product product, User user);
} 
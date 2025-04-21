package com.ecommerce.products.repository;

import com.ecommerce.products.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findByProductId(Long productId);
    
    List<ProductReview> findByUserId(Long userId);
    
    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.product.id = :productId")
    Double getAverageRating(@Param("productId") Long productId);
    
    @Query("SELECT COUNT(r) FROM ProductReview r WHERE r.product.id = :productId")
    Long getReviewCount(@Param("productId") Long productId);

    List<ProductReview> findByUserEmail(String userEmail);

    Optional<ProductReview> findByIdAndProductId(Long id, Long productId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);
} 
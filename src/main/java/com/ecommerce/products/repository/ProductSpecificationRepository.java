package com.ecommerce.products.repository;

import com.ecommerce.products.entity.ProductSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductSpecificationRepository extends JpaRepository<ProductSpecification, Long> {
    List<ProductSpecification> findByProductId(Long productId);
    
    @Query("SELECT s FROM ProductSpecification s WHERE s.product.id = :productId ORDER BY s.displayOrder")
    List<ProductSpecification> findByProductIdOrdered(@Param("productId") Long productId);
} 
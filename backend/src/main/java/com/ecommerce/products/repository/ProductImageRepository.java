package com.ecommerce.products.repository;

import com.ecommerce.products.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductId(Long productId);
    List<ProductImage> findByProductIdAndIsMain(Long productId, boolean isMain);
    void deleteByProductId(Long productId);
} 
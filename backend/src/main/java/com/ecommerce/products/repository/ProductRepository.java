package com.ecommerce.products.repository;

import com.ecommerce.products.entity.Product;
import com.ecommerce.products.specification.ProductSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByCategoryId(Long categoryId);
    
    List<Product> findByNameContainingIgnoreCase(String name);
    
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    
    @Query("SELECT p FROM Product p WHERE p.active = true")
    List<Product> findAllActive();
    
    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0")
    List<Product> findAllInStock();
    
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.active = true")
    List<Product> findActiveByCategoryId(@Param("categoryId") Long categoryId);

    default Page<Product> findAll(ProductSpecification specification, Pageable pageable) {
        return findAll((Specification<Product>) specification, pageable);
    }
} 
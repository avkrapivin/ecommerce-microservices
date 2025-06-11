package com.ecommerce.shipping.repository;

import com.ecommerce.shipping.model.ShippingInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingInfoRepository extends JpaRepository<ShippingInfo, Long> {
    Optional<ShippingInfo> findByOrderId(String orderId);
    boolean existsByOrderId(String orderId);
} 
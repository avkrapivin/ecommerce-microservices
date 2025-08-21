package com.ecommerce.shipping.repository;

import com.ecommerce.shipping.model.ShippingInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingInfoRepository extends JpaRepository<ShippingInfo, Long> {
    Optional<ShippingInfo> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);
    
    @Query("SELECT s FROM ShippingInfo s WHERE s.trackingNumber = :trackingNumber OR s.shippoShipmentId = :shipmentId")
    Optional<ShippingInfo> findByTrackingNumberOrShippoShipmentId(@Param("trackingNumber") String trackingNumber, 
                                                                  @Param("shipmentId") String shipmentId);
} 
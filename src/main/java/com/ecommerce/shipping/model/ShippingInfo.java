package com.ecommerce.shipping.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "shipping_info")
public class ShippingInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "shippo_shipment_id")
    private String shippoShipmentId;

    @Column(name = "shippo_rate_id")
    private String shippoRateId;

    @Column(name = "shippo_transaction_id")
    private String shippoTransactionId;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "tracking_url")
    private String trackingUrl;

    @Column(name = "label_url")
    private String labelUrl;

    @Column(name = "carrier")
    private String carrier;

    @Column(name = "service")
    private String service;

    @Column(name = "amount")
    private String amount;

    @Column(name = "currency")
    private String currency;

    @Column(name = "estimated_days")
    private String estimatedDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ShippingStatus status;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
} 
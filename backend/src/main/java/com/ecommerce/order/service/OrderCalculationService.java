package com.ecommerce.order.service;

import org.springframework.stereotype.Service;

import com.ecommerce.order.entity.Order;

import java.math.BigDecimal;

@Service
public class OrderCalculationService {
    
    public BigDecimal calculateShippingCost(Order order) {
        // Примерная логика расчета стоимости доставки
        return order.getSubtotal().compareTo(BigDecimal.valueOf(100)) > 0 ? BigDecimal.ZERO : BigDecimal.valueOf(10);
    }

    public BigDecimal calculateTax(Order order) {
        // Примерная логика расчета налога (10%)
        return order.getSubtotal().multiply(BigDecimal.valueOf(0.1));
    }

    public BigDecimal calculateTotal(Order order, BigDecimal shippingCost, BigDecimal tax) {
        return order.getSubtotal().add(shippingCost).add(tax);
    }
} 
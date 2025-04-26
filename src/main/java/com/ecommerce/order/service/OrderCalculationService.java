package com.ecommerce.order.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderCalculationService {
    
    public BigDecimal calculateShippingCost(BigDecimal subtotal) {
        // Примерная логика расчета стоимости доставки
        return subtotal.compareTo(BigDecimal.valueOf(100)) > 0 ? BigDecimal.ZERO : BigDecimal.valueOf(10);
    }

    public BigDecimal calculateTax(BigDecimal subtotal) {
        // Примерная логика расчета налога (10%)
        return subtotal.multiply(BigDecimal.valueOf(0.1));
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal shippingCost, BigDecimal tax) {
        return subtotal.add(shippingCost).add(tax);
    }
} 
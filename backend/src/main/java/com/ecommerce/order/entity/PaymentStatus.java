package com.ecommerce.order.entity;

public enum PaymentStatus {
    PENDING,           // Ожидает оплаты
    PROCESSING,        // Платеж в обработке
    COMPLETED,         // Платеж успешно завершен
    FAILED,            // Платеж не удался
    REFUNDED,          // Платеж возвращен
    PARTIALLY_REFUNDED // Частичный возврат
} 
package com.ecommerce.order.entity;

public enum OrderStatus {
    PENDING,           // Заказ создан, ожидает обработки
    CONFIRMED,         // Заказ подтвержден, товары списаны
    PROCESSING,        // Заказ в обработке
    PAID,             // Заказ оплачен
    SHIPPED,          // Заказ отправлен
    DELIVERED,        // Заказ доставлен
    CANCELLED,        // Заказ отменен
    REFUNDED          // Заказ возвращен
} 
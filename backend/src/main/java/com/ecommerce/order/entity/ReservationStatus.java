package com.ecommerce.order.entity;

public enum ReservationStatus {
    ACTIVE,      // Активное резервирование
    EXPIRED,     // Истекло
    CONFIRMED,   // Подтверждено (преобразовано в заказ)
    CANCELLED    // Отменено
}

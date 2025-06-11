package com.ecommerce.shipping.model;

public enum ShippingStatus {
    PENDING,        // Ожидает обработки
    PROCESSING,     // В процессе
    LABEL_CREATED,  // Лейбл создан
    SHIPPED,        // Отправлено
    IN_TRANSIT,     // В пути
    DELIVERED,      // Доставлено
    FAILED,         // Ошибка
    CANCELLED       // Отменено
} 
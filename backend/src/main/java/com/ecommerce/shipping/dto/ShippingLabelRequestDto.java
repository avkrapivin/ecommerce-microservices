package com.ecommerce.shipping.dto;

import lombok.Data;

@Data
public class ShippingLabelRequestDto {
    private String rateId; // ID тарифа, выбранного клиентом
    private String orderId; // ID заказа в нашей системе
    private String labelFormat; // Формат лейбла (PDF, PNG, ZPL)
    private String labelSize; // Размер лейбла (4x6, 4x8, etc.)
    private boolean async; // Асинхронная генерация лейбла
} 
package com.ecommerce.shipping.dto;

import lombok.Data;

import java.util.List;

@Data
public class ShippingRateRequestDto {
    private ShippoAddressDto addressFrom;
    private ShippoAddressDto addressTo;
    private List<ShippoParcelDto> parcels;
    private List<String> carriers; // Опционально: список перевозчиков для расчета
    private List<String> services; // Опционально: список сервисов для расчета
    private String currency; // Опционально: валюта для расчета (по умолчанию USD)
} 
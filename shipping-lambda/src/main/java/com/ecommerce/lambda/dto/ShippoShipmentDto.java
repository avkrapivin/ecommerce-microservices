package com.ecommerce.lambda.dto;

import lombok.Data;
import java.util.List;

@Data
public class ShippoShipmentDto {
    private ShippoAddressDto addressFrom;
    private ShippoAddressDto addressTo;
    private List<ShippoParcelDto> parcels;
    private boolean async = false;
} 
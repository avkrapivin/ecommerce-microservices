package com.ecommerce.lambda.dto;

import lombok.Data;

@Data
public class ShippoParcelDto {
    private double length;
    private double width;
    private double height;
    private double weight;
    private String distanceUnit;
    private String massUnit;
} 
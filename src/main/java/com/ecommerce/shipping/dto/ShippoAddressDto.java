package com.ecommerce.shipping.dto;

import lombok.Data;

@Data
public class ShippoAddressDto {
    private String name;
    private String street1;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String phone;
    private boolean validate = true;
} 
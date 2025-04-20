package com.ecommerce.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileUpdateDto {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private AddressDto address;
} 
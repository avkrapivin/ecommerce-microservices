package com.ecommerce.order.dto;

import com.ecommerce.user.dto.AddressDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShippingAddressDto {
    private Long id;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Address is required")
    @Valid
    private AddressDto address;

    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;
} 
package com.ecommerce.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserConfirmResetDto {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String code;

    @NotBlank
    private String newPassword;
} 
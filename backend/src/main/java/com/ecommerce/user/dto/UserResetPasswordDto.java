package com.ecommerce.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserResetPasswordDto {
    @Email
    @NotBlank
    private String email;
} 
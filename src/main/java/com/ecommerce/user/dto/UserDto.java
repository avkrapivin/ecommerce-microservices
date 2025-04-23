package com.ecommerce.user.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String cognitoId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 
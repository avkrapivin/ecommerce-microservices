package com.ecommerce.admin.dto;

import com.ecommerce.user.entity.Role;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminUserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String cognitoId;
    private Role role;
    private boolean enabled;
    
    // Статистика для админа
    private Integer totalOrders;
    private BigDecimal totalSpent;
    private LocalDateTime lastLoginAt;
    private String registrationIP;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Адрес (если есть)
    private String address;
    private String city;
    private String country;
} 
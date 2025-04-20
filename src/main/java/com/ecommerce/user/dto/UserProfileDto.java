package com.ecommerce.user.dto;

import com.ecommerce.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private AddressDto address;
    private boolean enabled;
    private String createdAt;
    private String updatedAt;
} 
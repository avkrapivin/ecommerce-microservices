package com.ecommerce.user.service;

import com.ecommerce.user.dto.*;
import com.ecommerce.user.entity.Role;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.exception.UserAuthenticationException;
import com.ecommerce.user.exception.UserRegistrationException;
import com.ecommerce.user.exception.UserNotFoundException;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.security.util.JwtUtil;
import com.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final CognitoService cognitoService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void registerUser(UserRegistrationDto registrationDto) {
        try {
            log.info("Registering user with email: {}", registrationDto.getEmail());
            
            // Регистрируем пользователя в Cognito
            cognitoService.registerUser(
                registrationDto.getEmail(),
                registrationDto.getPassword(),
                registrationDto.getFirstName(),
                registrationDto.getLastName()
            );
            
            // Получаем cognito_id для пользователя
            String cognitoId = cognitoService.getUserCognitoId(registrationDto.getEmail());
            
            // Создаем пользователя в базе данных
            User user = new User();
            user.setEmail(registrationDto.getEmail());
            user.setFirstName(registrationDto.getFirstName());
            user.setLastName(registrationDto.getLastName());
            user.setEnabled(false); // Пользователь не активирован до подтверждения email
            user.setCognitoId(cognitoId);
            
            userRepository.save(user);
            log.info("User registered successfully: {}", registrationDto.getEmail());
        } catch (Exception e) {
            log.error("Failed to register user: {}", e.getMessage());
            throw new RuntimeException("Failed to register user: " + e.getMessage());
        }
    }

    @Transactional
    public String confirmRegistration(String email, String confirmationCode) {
        try {                        
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
            
            cognitoService.confirmUser(email, confirmationCode);
            
            user.setEnabled(true);
            userRepository.save(user);
            
            log.info("User confirmed successfully: {}", email);
            return "Registration confirmed successfully. You can now log in.";
        } catch (Exception e) {
            log.error("Failed to confirm user: {}", e.getMessage());
            throw new RuntimeException("Failed to confirm user: " + e.getMessage());
        }
    }

    public TokenResponse loginUser(UserLoginDto loginDto) {
        return cognitoService.loginUser(loginDto.getEmail(), loginDto.getPassword());
    }

    public void resendConfirmationCode(String email) {
        cognitoService.resendConfirmationCode(email);
    }

    @Cacheable(value = "userProfile", key = "#token")
    public UserProfileDto getProfile(String token) {
        String cognitoId = cognitoService.extractCognitoIdFromToken(token);
        String email = cognitoService.getUserEmail(cognitoId);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserAuthenticationException("User not found"));

        return mapToProfileDto(user);
    }

    @Transactional
    @CacheEvict(value = "userProfile", key = "#token")
    public void updateProfile(String token, UserProfileUpdateDto updateDto) {
        String cognitoId = cognitoService.extractCognitoIdFromToken(token);
        String email = cognitoService.getUserEmail(cognitoId);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserAuthenticationException("User not found"));

        user.setFirstName(updateDto.getFirstName());
        user.setLastName(updateDto.getLastName());
        if (updateDto.getAddress() != null) {
            user.setAddress(mapToAddress(updateDto.getAddress()));
        }

        userRepository.save(user);
    }

    public void resetPassword(String email) {
        try {
            cognitoService.resetPassword(email);
        } catch (Exception e) {
            throw new UserRegistrationException("Failed to reset password: " + e.getMessage());
        }
    }

    public void confirmPasswordReset(String email, String code, String newPassword) {
        try {
            cognitoService.confirmResetPassword(email, code, newPassword);
        } catch (Exception e) {
            throw new UserRegistrationException("Failed to confirm password reset: " + e.getMessage());
        }
    }

    public TokenResponse refreshToken(String refreshToken) {
        try {
            return cognitoService.refreshToken(refreshToken);
        } catch (Exception e) {
            throw new UserAuthenticationException("Failed to refresh token: " + e.getMessage());
        }
    }

    private UserProfileDto mapToProfileDto(User user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt().toString());
        dto.setUpdatedAt(user.getUpdatedAt().toString());
        
        if (user.getAddress() != null) {
            dto.setAddress(mapToAddressDto(user.getAddress()));
        }
        
        return dto;
    }

    private AddressDto mapToAddressDto(com.ecommerce.user.entity.Address address) {
        AddressDto dto = new AddressDto();
        dto.setStreet(address.getStreet());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setCountry(address.getCountry());
        dto.setZipCode(address.getZipCode());
        return dto;
    }

    private com.ecommerce.user.entity.Address mapToAddress(AddressDto dto) {
        com.ecommerce.user.entity.Address address = new com.ecommerce.user.entity.Address();
        address.setStreet(dto.getStreet());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setCountry(dto.getCountry());
        address.setZipCode(dto.getZipCode());
        return address;
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(String email) {
        log.info("Getting user profile for email: {}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        
        return mapToUserProfileDto(user);
    }

    @Transactional
    public void updateUserProfile(String email, UserProfileDto profileDto) {
        log.info("Updating user profile for email: {}", email);
        
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        
        user.setFirstName(profileDto.getFirstName());
        user.setLastName(profileDto.getLastName());
        user.setUpdatedAt(LocalDateTime.now());
        
        userRepository.save(user);
        log.info("User profile updated successfully: {}", email);
    }

    private UserProfileDto mapToUserProfileDto(User user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt().toString());
        dto.setUpdatedAt(user.getUpdatedAt().toString());
        
        if (user.getAddress() != null) {
            dto.setAddress(mapToAddressDto(user.getAddress()));
        }
        
        return dto;
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    // Admin methods
    @Transactional(readOnly = true)
    public List<UserProfileDto> getAllUsers(int page, int size) {
        return userRepository.findAll().stream()
                .map(this::convertToUserProfileDto)
                .toList();
    }

    @Transactional
    public void updateUserRole(Long userId, String roleString) {
        User user = getUserById(userId);
        Role role = Role.valueOf(roleString.toUpperCase());
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional  
    public void toggleUserStatus(Long userId) {
        User user = getUserById(userId);
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    private UserProfileDto convertToUserProfileDto(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt().toString())
                .updatedAt(user.getUpdatedAt().toString())
                .build();
    }
} 
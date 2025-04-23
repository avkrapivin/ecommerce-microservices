package com.ecommerce.user.service;

import com.ecommerce.user.dto.*;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.security.util.JwtUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private CognitoService cognitoService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUser_ShouldRegisterUserSuccessfully() {
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setEmail("new@example.com");
        registrationDto.setPassword("password123");
        registrationDto.setFirstName("New");
        registrationDto.setLastName("User");

        when(cognitoService.getUserCognitoId(anyString())).thenReturn("test-cognito-id");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // When
        userService.registerUser(registrationDto);

        // Then
        verify(cognitoService, times(1)).registerUser(
            registrationDto.getEmail(),
            registrationDto.getPassword(),
            registrationDto.getFirstName(),
            registrationDto.getLastName()
        );
        verify(cognitoService, times(1)).getUserCognitoId(registrationDto.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void confirmRegistration_ShouldConfirmUserSuccessfully() {
        // Given
        String email = "test@example.com";
        String confirmationCode = "123456";
        User user = new User();
        user.setEmail(email);
        user.setCognitoId("test-cognito-id");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        doNothing().when(cognitoService).confirmUser(email, confirmationCode);

        // When
        String result = userService.confirmRegistration(email, confirmationCode);

        // Then
        assertEquals("Registration confirmed successfully. You can now log in.", result);
        verify(cognitoService, times(1)).confirmUser(email, confirmationCode);
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, times(1)).save(user);
        assertTrue(user.isEnabled());
    }

    @Test
    void confirmRegistration_ShouldThrowException_WhenUserNotFound() {
        // Given
        String email = "nonexistent@example.com";
        String confirmationCode = "123456";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            userService.confirmRegistration(email, confirmationCode)
        );
        verify(userRepository, times(1)).findByEmail(email);
        verify(cognitoService, never()).confirmUser(anyString(), anyString());
    }

    @Test
    void getUserProfile_ShouldReturnUserProfile() {
        // Given
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setCognitoId("test-cognito-id");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // When
        UserProfileDto profile = userService.getUserProfile(email);

        // Then
        assertNotNull(profile);
        assertEquals(email, profile.getEmail());
        assertEquals("Test", profile.getFirstName());
        assertEquals("User", profile.getLastName());
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void updateUserProfile_ShouldUpdateProfile() {
        // Given
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Old");
        user.setLastName("Name");

        UserProfileDto updateDto = new UserProfileDto();
        updateDto.setFirstName("New");
        updateDto.setLastName("Name");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.updateUserProfile(email, updateDto);

        // Then
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, times(1)).save(user);
        assertEquals("New", user.getFirstName());
        assertEquals("Name", user.getLastName());
    }

    @Test
    void getUserByEmail_ShouldReturnUser() {
        // Given
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // When
        User result = userService.getUserByEmail(email);

        // Then
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void getUserByEmail_ShouldThrowException_WhenUserNotFound() {
        // Given
        String email = "nonexistent@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> 
            userService.getUserByEmail(email)
        );
        verify(userRepository, times(1)).findByEmail(email);
    }
} 
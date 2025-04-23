package com.ecommerce.user.service;

import com.ecommerce.user.UserIntegrationTest;
import com.ecommerce.user.dto.UserProfileDto;
import com.ecommerce.user.dto.UserRegistrationDto;
import com.ecommerce.user.dto.UserLoginDto;
import com.ecommerce.user.dto.UserProfileUpdateDto;
import com.ecommerce.user.entity.User;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@ExtendWith(MockitoExtension.class)
class UserServiceTest extends UserIntegrationTest {
    @Autowired
    private UserService userService;

    @Mock
    private CognitoService cognitoService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService mockedUserService;

    @Test
    void getUserById_ShouldReturnUser() {
        User user = userService.getUserById(testUser.getId());
        
        assertNotNull(user);
        assertEquals(testUser.getEmail(), user.getEmail());
        assertEquals(testUser.getFirstName(), user.getFirstName());
        assertEquals(testUser.getLastName(), user.getLastName());
    }

    @Test
    void getUserByEmail_ShouldReturnUser() {
        User user = userService.getUserByEmail(testUser.getEmail());
        
        assertNotNull(user);
        assertEquals(testUser.getId(), user.getId());
        assertEquals(testUser.getFirstName(), user.getFirstName());
        assertEquals(testUser.getLastName(), user.getLastName());
    }

    @Test
    void getUserProfile_ShouldReturnUserProfile() {
        UserProfileDto profile = userService.getUserProfile(testUser.getEmail());
        
        assertNotNull(profile);
        assertEquals(testUser.getEmail(), profile.getEmail());
        assertEquals(testUser.getFirstName(), profile.getFirstName());
        assertEquals(testUser.getLastName(), profile.getLastName());
    }

    @Test
    void updateUserProfile_ShouldUpdateProfile() {
        UserProfileDto updateDto = new UserProfileDto();
        updateDto.setFirstName("Updated");
        updateDto.setLastName("Name");
        
        userService.updateUserProfile(testUser.getEmail(), updateDto);
        
        UserProfileDto updatedProfile = userService.getUserProfile(testUser.getEmail());
        assertEquals("Updated", updatedProfile.getFirstName());
        assertEquals("Name", updatedProfile.getLastName());
    }

    @Test
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> 
            userService.getUserById(999L)
        );
    }

    @Test
    void getUserByEmail_ShouldThrowException_WhenUserNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> 
            userService.getUserByEmail("nonexistent@example.com")
        );
    }

    @Test
    void registerUser_ShouldRegisterUserSuccessfully() {
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setEmail("newuser@example.com");
        registrationDto.setPassword("password123");
        registrationDto.setFirstName("New");
        registrationDto.setLastName("User");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail(registrationDto.getEmail());
        savedUser.setFirstName(registrationDto.getFirstName());
        savedUser.setLastName(registrationDto.getLastName());
        savedUser.setCognitoId("test-cognito-id");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        doNothing().when(cognitoService).registerUser(
            anyString(), anyString(), anyString(), anyString()
        );
        when(cognitoService.getUserCognitoId(anyString())).thenReturn("test-cognito-id");

        // When
        mockedUserService.registerUser(registrationDto);

        // Then
        verify(cognitoService, times(1)).registerUser(
            registrationDto.getEmail(),
            registrationDto.getPassword(),
            registrationDto.getFirstName(),
            registrationDto.getLastName()
        );
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
        mockedUserService.confirmRegistration(email, confirmationCode);

        // Then
        verify(cognitoService, times(1)).confirmUser(email, confirmationCode);
        verify(userRepository, times(1)).findByEmail(email);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void confirmRegistration_ShouldThrowException_WhenUserNotFound() {
        // Given
        String email = "nonexistent@example.com";
        String confirmationCode = "123456";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> 
            mockedUserService.confirmRegistration(email, confirmationCode)
        );
        verify(userRepository, times(1)).findByEmail(email);
        verify(cognitoService, never()).confirmUser(anyString(), anyString());
    }
} 
package com.ecommerce.user.controller;

import com.ecommerce.order.event.OrderEventPublisher;
import com.ecommerce.security.util.JwtUtil;
import com.ecommerce.user.UserIntegrationTest;
import com.ecommerce.user.dto.*;
import com.ecommerce.user.service.CognitoService;
import com.ecommerce.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
class UserControllerTest extends UserIntegrationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private CognitoService cognitoService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private OrderEventPublisher orderEventPublisher;

    @Test
    void registerUser_ShouldRegisterUserSuccessfully() throws Exception {
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setEmail("new@example.com");
        registrationDto.setPassword("password123");
        registrationDto.setFirstName("New");
        registrationDto.setLastName("User");

        doNothing().when(userService).registerUser(any(UserRegistrationDto.class));

        // When & Then
        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        verify(userService, times(1)).registerUser(any(UserRegistrationDto.class));
    }

    @Test
    void confirmRegistration_ShouldConfirmUserSuccessfully() throws Exception {
        // Given
        UserConfirmationDto confirmationDto = new UserConfirmationDto();
        confirmationDto.setEmail("test@example.com");
        confirmationDto.setConfirmationCode("123456");

        when(userService.confirmRegistration(anyString(), anyString()))
                .thenReturn("Registration confirmed successfully. You can now log in.");

        // When & Then
        mockMvc.perform(post("/users/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmationDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(userService, times(1)).confirmRegistration(anyString(), anyString());
    }

    @Test
    void loginUser_ShouldReturnTokens() throws Exception {
        // Given
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setEmail("test@example.com");
        loginDto.setPassword("password123");

        TokenResponse tokenResponse = new TokenResponse("access-token", "refresh-token", "id-token");
        when(userService.loginUser(any(UserLoginDto.class))).thenReturn(tokenResponse);

        // When & Then
        mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.idToken").value("id-token"));

        verify(userService, times(1)).loginUser(any(UserLoginDto.class));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getProfile_ShouldReturnUserProfile() throws Exception {
        // Given
        UserProfileDto profileDto = new UserProfileDto();
        profileDto.setEmail("test@example.com");
        profileDto.setFirstName("Test");
        profileDto.setLastName("User");

        when(jwtUtil.extractCognitoIdFromToken(anyString())).thenReturn("cognito-id");
        when(cognitoService.getUserCognitoId(anyString())).thenReturn("cognito-id");
        when(userService.getProfile(anyString())).thenReturn(profileDto);

        // When & Then
        mockMvc.perform(get("/users/profile")
                .header("Authorization", "Bearer test.token.value"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"));

        verify(userService, times(1)).getProfile(anyString());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void updateProfile_ShouldUpdateUserProfile() throws Exception {
        // Given
        UserProfileUpdateDto updateDto = new UserProfileUpdateDto();
        updateDto.setFirstName("Updated");
        updateDto.setLastName("Name");

        when(jwtUtil.extractCognitoIdFromToken(anyString())).thenReturn("cognito-id");
        when(cognitoService.getUserCognitoId(anyString())).thenReturn("cognito-id");
        doNothing().when(userService).updateProfile(anyString(), any(UserProfileUpdateDto.class));

        // When & Then
        mockMvc.perform(put("/users/profile")
                .header("Authorization", "Bearer test.token.value")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk());

        verify(userService, times(1)).updateProfile(anyString(), any(UserProfileUpdateDto.class));
    }

    @Test
    void resetPassword_ShouldInitiatePasswordReset() throws Exception {
        // Given
        UserResetPasswordDto resetDto = new UserResetPasswordDto();
        resetDto.setEmail("test@example.com");

        doNothing().when(userService).resetPassword(anyString());

        // When & Then
        mockMvc.perform(post("/users/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetDto)))
                .andExpect(status().isOk());

        verify(userService, times(1)).resetPassword(anyString());
    }

    @Test
    void confirmPasswordReset_ShouldResetPassword() throws Exception {
        // Given
        UserConfirmResetDto confirmDto = new UserConfirmResetDto();
        confirmDto.setEmail("test@example.com");
        confirmDto.setCode("123456");
        confirmDto.setNewPassword("newPassword123");

        doNothing().when(userService).confirmPasswordReset(anyString(), anyString(), anyString());

        // When & Then
        mockMvc.perform(post("/users/confirm-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmDto)))
                .andExpect(status().isOk());

        verify(userService, times(1)).confirmPasswordReset(anyString(), anyString(), anyString());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void refreshToken_ShouldReturnNewTokens() throws Exception {
        // Given
        UserRefreshTokenDto refreshDto = new UserRefreshTokenDto();
        refreshDto.setRefreshToken("test-refresh-token");

        TokenResponse tokenResponse = new TokenResponse("new-access-token", "new-refresh-token", "new-id-token");
        when(userService.refreshToken(anyString())).thenReturn(tokenResponse);

        // When & Then
        mockMvc.perform(post("/users/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.idToken").value("new-id-token"));

        verify(userService, times(1)).refreshToken(anyString());
    }
} 
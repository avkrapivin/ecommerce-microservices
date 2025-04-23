package com.ecommerce.user.service;

import com.ecommerce.user.dto.TokenResponse;
import com.ecommerce.user.exception.UserAuthenticationException;
import com.ecommerce.user.exception.UserRegistrationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CognitoServiceUnitTest {
    @Mock
    private CognitoIdentityProviderClient cognitoClient;

    @InjectMocks
    private CognitoService cognitoService;

    @Test
    void registerUser_ShouldRegisterUserSuccessfully() {
        // Given
        String email = "test@example.com";
        String password = "password123";
        String firstName = "Test";
        String lastName = "User";

        when(cognitoClient.signUp(any(SignUpRequest.class))).thenReturn(SignUpResponse.builder().build());

        // When
        cognitoService.registerUser(email, password, firstName, lastName);

        // Then
        verify(cognitoClient, times(1)).signUp(any(SignUpRequest.class));
    }

    @Test
    void registerUser_ShouldThrowException_WhenUserExists() {
        // Given
        String email = "existing@example.com";
        String password = "password123";
        String firstName = "Test";
        String lastName = "User";

        when(cognitoClient.signUp(any(SignUpRequest.class)))
                .thenThrow(UsernameExistsException.builder().build());

        // When & Then
        assertThrows(UserRegistrationException.class, () ->
            cognitoService.registerUser(email, password, firstName, lastName)
        );
        verify(cognitoClient, times(1)).signUp(any(SignUpRequest.class));
    }

    @Test
    void confirmUser_ShouldConfirmUserSuccessfully() {
        // Given
        String email = "test@example.com";
        String confirmationCode = "123456";

        when(cognitoClient.confirmSignUp(any(ConfirmSignUpRequest.class)))
                .thenReturn(ConfirmSignUpResponse.builder().build());

        // When
        cognitoService.confirmUser(email, confirmationCode);

        // Then
        verify(cognitoClient, times(1)).confirmSignUp(any(ConfirmSignUpRequest.class));
    }

    @Test
    void confirmUser_ShouldThrowException_WhenCodeInvalid() {
        // Given
        String email = "test@example.com";
        String confirmationCode = "invalid";

        when(cognitoClient.confirmSignUp(any(ConfirmSignUpRequest.class)))
                .thenThrow(CodeMismatchException.builder().build());

        // When & Then
        assertThrows(UserRegistrationException.class, () ->
            cognitoService.confirmUser(email, confirmationCode)
        );
        verify(cognitoClient, times(1)).confirmSignUp(any(ConfirmSignUpRequest.class));
    }

    @Test
    void loginUser_ShouldReturnTokens() {
        // Given
        String email = "test@example.com";
        String password = "password123";

        AuthenticationResultType authResult = AuthenticationResultType.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .idToken("id-token")
                .build();

        InitiateAuthResponse authResponse = InitiateAuthResponse.builder()
                .authenticationResult(authResult)
                .build();

        when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                .thenReturn(authResponse);

        // When
        TokenResponse result = cognitoService.loginUser(email, password);

        // Then
        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals("id-token", result.getIdToken());
        verify(cognitoClient, times(1)).initiateAuth(any(InitiateAuthRequest.class));
    }

    @Test
    void loginUser_ShouldThrowException_WhenCredentialsInvalid() {
        // Given
        String email = "test@example.com";
        String password = "wrong-password";

        when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                .thenThrow(NotAuthorizedException.builder().build());

        // When & Then
        assertThrows(UserAuthenticationException.class, () ->
            cognitoService.loginUser(email, password)
        );
        verify(cognitoClient, times(1)).initiateAuth(any(InitiateAuthRequest.class));
    }

    @Test
    void refreshToken_ShouldReturnNewTokens() {
        // Given
        String refreshToken = "test-refresh-token";

        AuthenticationResultType authResult = AuthenticationResultType.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .idToken("new-id-token")
                .build();

        AdminInitiateAuthResponse authResponse = AdminInitiateAuthResponse.builder()
                .authenticationResult(authResult)
                .build();

        when(cognitoClient.adminInitiateAuth(any(AdminInitiateAuthRequest.class)))
                .thenReturn(authResponse);

        // When
        TokenResponse result = cognitoService.refreshToken(refreshToken);

        // Then
        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
        assertEquals("new-id-token", result.getIdToken());
        verify(cognitoClient, times(1)).adminInitiateAuth(any(AdminInitiateAuthRequest.class));
    }
} 
package com.ecommerce.user.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ecommerce.config.AwsConfig;
import com.ecommerce.user.dto.TokenResponse;
import com.ecommerce.user.exception.UserAuthenticationException;
import com.ecommerce.user.exception.UserRegistrationException;
import com.ecommerce.user.util.SecretHashCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CognitoService {
    private final CognitoIdentityProviderClient cognitoClient;
    private final AwsConfig awsConfig;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Value("${aws.cognito.client-id}")
    private String clientId;

    @Value("${aws.cognito.client-secret}")
    private String clientSecret;

    public void registerUser(String email, String password, String firstName, String lastName) {
        try {
            log.info("Attempting to register user with email: {}", email);
            
            SignUpRequest.Builder signUpRequestBuilder = SignUpRequest.builder()
                    .clientId(clientId)
                    .username(email)
                    .password(password)
                    .userAttributes(
                            AttributeType.builder().name("email").value(email).build(),
                            AttributeType.builder().name("given_name").value(firstName).build(),
                            AttributeType.builder().name("family_name").value(lastName).build()
                    );

            if (clientSecret != null && !clientSecret.isEmpty()) {
                signUpRequestBuilder.secretHash(calculateSecretHash(clientId, clientSecret, email));
            }

            cognitoClient.signUp(signUpRequestBuilder.build());
            log.info("User registration successful for email: {}", email);
        } catch (UsernameExistsException e) {
            log.error("User already exists: {}", email);
            throw new UserRegistrationException("User with this email already exists");
        } catch (InvalidPasswordException e) {
            log.error("Invalid password format for user: {}", email);
            throw new UserRegistrationException("Password does not meet requirements");
        } catch (Exception e) {
            log.error("Failed to register user: {}", e.getMessage());
            throw new UserRegistrationException("Failed to register user: " + e.getMessage());
        }
    }

    public void confirmUser(String email, String confirmationCode) {
        try {
            log.info("Confirming user registration: {}", email);
            
            ConfirmSignUpRequest.Builder confirmRequestBuilder = ConfirmSignUpRequest.builder()
                    .clientId(clientId)
                    .username(email)
                    .confirmationCode(confirmationCode);

            if (clientSecret != null && !clientSecret.isEmpty()) {
                confirmRequestBuilder.secretHash(calculateSecretHash(clientId, clientSecret, email));
            }

            cognitoClient.confirmSignUp(confirmRequestBuilder.build());
            log.info("User confirmation successful: {}", email);
        } catch (CodeMismatchException e) {
            log.error("Invalid confirmation code for user: {}", email);
            throw new UserRegistrationException("Invalid confirmation code");
        } catch (ExpiredCodeException e) {
            log.error("Expired confirmation code for user: {}", email);
            throw new UserRegistrationException("Confirmation code has expired");
        } catch (Exception e) {
            log.error("Failed to confirm user: {}", e.getMessage());
            throw new UserRegistrationException("Failed to confirm user: " + e.getMessage());
        }
    }

    public void resendConfirmationCode(String email) {
        try {
            log.info("Resending confirmation code to: {}", email);
            
            ResendConfirmationCodeRequest.Builder requestBuilder = ResendConfirmationCodeRequest.builder()
                    .clientId(clientId)
                    .username(email);

            if (clientSecret != null && !clientSecret.isEmpty()) {
                requestBuilder.secretHash(calculateSecretHash(clientId, clientSecret, email));
            }

            cognitoClient.resendConfirmationCode(requestBuilder.build());
            log.info("Confirmation code resent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to resend confirmation code: {}", e.getMessage());
            throw new UserRegistrationException("Failed to resend confirmation code: " + e.getMessage());
        }
    }

    public TokenResponse loginUser(String email, String password) {
        try {
            log.info("Attempting to login user: {}", email);
            
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", email);
            authParams.put("PASSWORD", password);
            
            if (clientSecret != null && !clientSecret.isEmpty()) {
                authParams.put("SECRET_HASH", calculateSecretHash(clientId, clientSecret, email));
            }

            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .clientId(clientId)
                    .authParameters(authParams)
                    .build();

            InitiateAuthResponse authResult = cognitoClient.initiateAuth(authRequest);
            
            if (authResult.challengeName() != null) {
                log.warn("Login requires additional challenge: {}", authResult.challengeName());
                throw new UserAuthenticationException("Additional authentication required");
            }

            AuthenticationResultType authenticationResult = authResult.authenticationResult();
            log.info("Login successful for user: {}", email);
            
            return new TokenResponse(
                    authenticationResult.accessToken(),
                    authenticationResult.refreshToken(),
                    authenticationResult.idToken()
            );
        } catch (NotAuthorizedException e) {
            log.error("Invalid credentials for user: {}", email);
            throw new UserAuthenticationException("Invalid email or password");
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", email);
            throw new UserAuthenticationException("User not found");
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            throw new UserAuthenticationException("Login failed: " + e.getMessage());
        }
    }

    public void resetPassword(String email) {
        try {
            log.info("Initiating password reset for user: {}", email);
            
            ForgotPasswordRequest.Builder requestBuilder = ForgotPasswordRequest.builder()
                    .clientId(clientId)
                    .username(email);

            if (clientSecret != null && !clientSecret.isEmpty()) {
                requestBuilder.secretHash(calculateSecretHash(clientId, clientSecret, email));
            }

            cognitoClient.forgotPassword(requestBuilder.build());
            log.info("Password reset initiated successfully for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to initiate password reset: {}", e.getMessage());
            throw new UserAuthenticationException("Failed to initiate password reset: " + e.getMessage());
        }
    }

    public void confirmResetPassword(String email, String code, String newPassword) {
        try {
            log.info("Confirming password reset for user: {}", email);
            
            ConfirmForgotPasswordRequest.Builder requestBuilder = ConfirmForgotPasswordRequest.builder()
                    .clientId(clientId)
                    .username(email)
                    .confirmationCode(code)
                    .password(newPassword);

            if (clientSecret != null && !clientSecret.isEmpty()) {
                requestBuilder.secretHash(calculateSecretHash(clientId, clientSecret, email));
            }

            cognitoClient.confirmForgotPassword(requestBuilder.build());
            log.info("Password reset confirmed successfully for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to confirm password reset: {}", e.getMessage());
            throw new UserAuthenticationException("Failed to confirm password reset: " + e.getMessage());
        }
    }

    public TokenResponse refreshToken(String refreshToken) {
        try {
            log.info("Attempting to refresh token");
            
            Map<String, String> authParams = new HashMap<>();
            authParams.put("REFRESH_TOKEN", refreshToken);
            
            if (clientSecret != null && !clientSecret.isEmpty()) {
                authParams.put("SECRET_HASH", calculateSecretHash(clientId, clientSecret, refreshToken));
            }

            AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                    .clientId(clientId)
                    .userPoolId(userPoolId)
                    .authParameters(authParams)
                    .build();

            AdminInitiateAuthResponse authResult = cognitoClient.adminInitiateAuth(authRequest);
            AuthenticationResultType authenticationResult = authResult.authenticationResult();
            
            log.info("Token refresh successful");
            return new TokenResponse(
                    authenticationResult.accessToken(),
                    authenticationResult.refreshToken(),
                    authenticationResult.idToken()
            );
        } catch (Exception e) {
            log.error("Failed to refresh token: {}", e.getMessage());
            throw new UserAuthenticationException("Failed to refresh token: " + e.getMessage());
        }
    }

    @Cacheable(value = "cognitoUser", key = "#token")
    public String extractCognitoIdFromToken(String token) {
        try {
            log.info("Extracting cognitoId from token");
            
            // Удаляем префикс "Bearer " если он есть
            String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            
            // Декодируем JWT токен
            DecodedJWT decodedJWT = JWT.decode(jwtToken);
            
            // Получаем sub claim, который содержит cognitoId
            String cognitoId = decodedJWT.getSubject();
            
            if (cognitoId == null || cognitoId.isEmpty()) {
                throw new UserAuthenticationException("Invalid token: sub claim is missing");
            }
            
            log.info("Successfully extracted cognitoId: {}", cognitoId);
            return cognitoId;
        } catch (Exception e) {
            log.error("Failed to extract cognitoId from token: {}", e.getMessage());
            throw new UserAuthenticationException("Failed to extract cognitoId from token: " + e.getMessage());
        }
    }

    @Cacheable(value = "cognitoUser", key = "#cognitoId")
    public String getUserEmail(String cognitoId) {
        try {
            AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
                .userPoolId(awsConfig.getCognito().getUserPoolId())
                .username(cognitoId)
                .build();

            AdminGetUserResponse userResponse = cognitoClient.adminGetUser(getUserRequest);
            return userResponse.userAttributes().stream()
                .filter(attr -> attr.name().equals("email"))
                .findFirst()
                .map(AttributeType::value)
                .orElseThrow(() -> new UserAuthenticationException("Email not found for user"));
        } catch (Exception e) {
            throw new UserAuthenticationException("Failed to get user email: " + e.getMessage());
        }
    }

    @Cacheable(value = "cognitoUser", key = "#email")
    public String getCognitoIdByEmail(String email) {
        try {
            ListUsersRequest listUsersRequest = ListUsersRequest.builder()
                .userPoolId(awsConfig.getCognito().getUserPoolId())
                .filter("email = \"" + email + "\"")
                .build();

            ListUsersResponse listUsersResponse = cognitoClient.listUsers(listUsersRequest);
            
            if (listUsersResponse.users().isEmpty()) {
                throw new UserAuthenticationException("User not found with email: " + email);
            }

            return listUsersResponse.users().get(0).username();
        } catch (Exception e) {
            throw new UserAuthenticationException("Failed to get cognitoId: " + e.getMessage());
        }
    }

    public String getUserCognitoId(String email) {
        try {
            AdminGetUserRequest request = AdminGetUserRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .build();

            AdminGetUserResponse response = cognitoClient.adminGetUser(request);
            return response.username(); // username в Cognito - это cognito_id
        } catch (Exception e) {
            log.error("Failed to get user cognitoId: {}", e.getMessage());
            throw new RuntimeException("Failed to get user cognitoId: " + e.getMessage());
        }
    }

    private String calculateSecretHash(String clientId, String clientSecret, String username) {
        try {
            SecretHashCalculator calculator = new SecretHashCalculator(clientId, clientSecret);
            return calculator.calculateSecretHash(username);
        } catch (Exception e) {
            log.error("Failed to calculate secret hash: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate secret hash", e);
        }
    }
} 
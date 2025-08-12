package com.ecommerce.user.controller;


import com.ecommerce.user.dto.*;
import com.ecommerce.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Profile("!local")
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        userService.registerUser(registrationDto);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmRegistration(@RequestBody @Valid UserConfirmationDto confirmationDto) {
        String message = userService.confirmRegistration(confirmationDto.getEmail(), confirmationDto.getConfirmationCode());
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<?> resendConfirmationCode(@Valid @RequestBody UserResetPasswordDto resendCodeDto) {
        userService.resendConfirmationCode(resendCodeDto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(@Valid @RequestBody UserLoginDto loginDto) {
        TokenResponse tokens = userService.loginUser(loginDto);

        boolean secure = false; // dev: false, prod: true (enable when deploying over HTTPS)

        ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.getAccessToken())
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofMinutes(15))
            .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.getRefreshToken())
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofDays(7))
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(tokens);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request
    ) {
        // Fallback to access_token cookie if Authorization header is not provided
        String token = Optional.ofNullable(authHeader)
                .filter(h -> !h.isBlank())
                .orElseGet(() -> {
                    if (request.getCookies() == null) return null;
                    return Arrays.stream(request.getCookies())
                            .filter(c -> "access_token".equals(c.getName()))
                            .map(c -> c.getValue())
                            .findFirst()
                            .orElse(null);
                });

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(userService.getProfile(token));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UserUpdateDto updateDto) {
        userService.updateProfile(token, updateDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody UserResetPasswordDto resetPasswordDto) {
        userService.resetPassword(resetPasswordDto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm-reset")
    public ResponseEntity<?> confirmPasswordReset(@Valid @RequestBody UserConfirmResetDto confirmResetDto) {
        userService.confirmPasswordReset(
            confirmResetDto.getEmail(),
            confirmResetDto.getCode(),
            confirmResetDto.getNewPassword()
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenResponse> refreshToken(
            @RequestBody(required = false) UserRefreshTokenDto refreshTokenDto,
            HttpServletRequest request
    ) {
        String refreshToken = null;
        if (refreshTokenDto != null && refreshTokenDto.getRefreshToken() != null && !refreshTokenDto.getRefreshToken().isBlank()) {
            refreshToken = refreshTokenDto.getRefreshToken();
        } else if (request.getCookies() != null) {
            refreshToken = Arrays.stream(request.getCookies())
                    .filter(c -> "refresh_token".equals(c.getName()))
                    .map(c -> c.getValue())
                    .findFirst()
                    .orElse(null);
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        TokenResponse tokens = userService.refreshToken(refreshToken);

        boolean secure = false; // dev: false, prod: true (enable when deploying over HTTPS)

        ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.getAccessToken())
            .httpOnly(true)
            .secure(secure)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofMinutes(15))
            .build();

        // Optionally rotate refresh token if service returns a new one
        ResponseCookie refreshCookie = null;
        if (tokens.getRefreshToken() != null && !tokens.getRefreshToken().isBlank()) {
            refreshCookie = ResponseCookie.from("refresh_token", tokens.getRefreshToken())
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();
        }

        var responseBuilder = ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, accessCookie.toString());
        if (refreshCookie != null) {
            responseBuilder.header(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }
        return responseBuilder.body(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Clear cookies by setting Max-Age=0
        ResponseCookie clearAccess = ResponseCookie.from("access_token", "")
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build();
        ResponseCookie clearRefresh = ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, clearAccess.toString())
            .header(HttpHeaders.SET_COOKIE, clearRefresh.toString())
            .build();
    }

    // Admin endpoints
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileDto>> getAllUsers(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ResponseEntity.ok(userService.getAllUsers(page, size));
    }

    @PutMapping("/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")  
    public ResponseEntity<Void> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String role = request.get("role");
        userService.updateUserRole(id, role);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/admin/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        return ResponseEntity.ok().build();
    }
} 
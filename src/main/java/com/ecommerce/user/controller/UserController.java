package com.ecommerce.user.controller;

import com.ecommerce.user.dto.*;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
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
    public ResponseEntity<?> resendConfirmationCode(@Valid @RequestBody UserResendCodeDto resendCodeDto) {
        userService.resendConfirmationCode(resendCodeDto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(@Valid @RequestBody UserLoginDto loginDto) {
        return ResponseEntity.ok(userService.loginUser(loginDto));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(userService.getProfile(token));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UserProfileUpdateDto updateDto) {
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
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody UserRefreshTokenDto refreshTokenDto) {
        return ResponseEntity.ok(userService.refreshToken(refreshTokenDto.getRefreshToken()));
    }
} 
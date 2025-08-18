package com.ecommerce.user.controller;

import com.ecommerce.user.dto.UserConfirmationDto;
import com.ecommerce.user.dto.UserConfirmResetDto;
import com.ecommerce.user.dto.UserLoginDto;
import com.ecommerce.user.dto.UserProfileDto;
import com.ecommerce.user.dto.UserRegistrationDto;
import com.ecommerce.user.dto.UserResetPasswordDto;
import com.ecommerce.user.dto.UserUpdateDto;
import com.ecommerce.user.entity.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Simplified user controller for local development without Cognito
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Profile("local")
public class LocalUserController {

    private final PasswordEncoder passwordEncoder;
    
    // Simple in-memory user storage for local development
    private static final Map<String, LocalUser> users = new HashMap<>();
    
    static {
        // Pre-create a test user
        users.put("test@example.com", new LocalUser(
            "test@example.com", 
            "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi", // "password"
            "Test",
            "User"
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        if (users.containsKey(registrationDto.getEmail())) {
            return ResponseEntity.badRequest().body("User already exists");
        }
        
        String encodedPassword = passwordEncoder.encode(registrationDto.getPassword());
        users.put(registrationDto.getEmail(), new LocalUser(
            registrationDto.getEmail(),
            encodedPassword,
            registrationDto.getFirstName(),
            registrationDto.getLastName()
        ));
        
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@Valid @RequestBody UserLoginDto loginDto) {
        LocalUser user = users.get(loginDto.getEmail());
        
        if (user == null || !passwordEncoder.matches(loginDto.getPassword(), user.password)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        
        // Generate simple tokens for local development
        String accessToken = "local-access-" + user.email + "-" + System.currentTimeMillis();
        String refreshToken = "local-refresh-" + user.email + "-" + System.currentTimeMillis();
        
        // Set HttpOnly cookies
        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofMinutes(15))
            .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
            .httpOnly(true)
            .secure(false)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ofDays(7))
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "message", "Login successful"
            ));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(HttpServletRequest request) {
        String accessToken = null;
        
        // Try to get token from cookies
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    break;
                }
            }
        }
        
        if (accessToken == null || !accessToken.startsWith("local-access-")) {
            return ResponseEntity.status(401).build();
        }
        
        // Extract email from token
        String tokenPart = accessToken.substring("local-access-".length());
        String[] parts = tokenPart.split("-");
        
        // Find the email part (everything before the last timestamp)
        if (parts.length < 2) {
            return ResponseEntity.status(401).build();
        }
        
        // Join all parts except the last one (timestamp) to reconstruct the email
        StringBuilder emailBuilder = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) emailBuilder.append("-");
            emailBuilder.append(parts[i]);
        }
        String email = emailBuilder.toString();
        LocalUser user = users.get(email);
        
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        
        UserProfileDto profile = new UserProfileDto();
        profile.setEmail(user.email);
        profile.setFirstName(user.firstName);
        profile.setLastName(user.lastName);
        profile.setRole(Role.USER);
        profile.setEnabled(true);
        
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Clear cookies
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

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmRegistration(@RequestBody @Valid UserConfirmationDto confirmationDto) {
        LocalUser user = users.get(confirmationDto.getEmail());
        
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        
        // For local development, accept any 6-digit code
        if (confirmationDto.getConfirmationCode().length() != 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid confirmation code"));
        }
        
        // In local development, we consider the user confirmed automatically
        // In production, this would validate against Cognito
        return ResponseEntity.ok(Map.of("message", "Registration confirmed successfully. You can now log in."));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<?> resendConfirmationCode(@Valid @RequestBody UserResetPasswordDto resendCodeDto) {
        LocalUser user = users.get(resendCodeDto.getEmail());
        
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        
        // For local development, just return success
        // In production, this would send a new code via Cognito
        return ResponseEntity.ok(Map.of("message", "Confirmation code sent successfully"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody UserResetPasswordDto resetPasswordDto) {
        LocalUser user = users.get(resetPasswordDto.getEmail());
        
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        
        // For local development, just return success
        // In production, this would send a reset code via Cognito
        return ResponseEntity.ok(Map.of("message", "Password reset code sent successfully"));
    }

    @PostMapping("/confirm-reset")
    public ResponseEntity<?> confirmPasswordReset(@Valid @RequestBody UserConfirmResetDto confirmResetDto) {
        LocalUser user = users.get(confirmResetDto.getEmail());
        
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        
        // For local development, accept any 6-digit code
        if (confirmResetDto.getCode().length() != 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid reset code"));
        }
        
        // Update password in local storage
        String encodedPassword = passwordEncoder.encode(confirmResetDto.getNewPassword());
        users.put(confirmResetDto.getEmail(), new LocalUser(
            user.email,
            encodedPassword,
            user.firstName,
            user.lastName
        ));
        
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(HttpServletRequest request, @Valid @RequestBody UserUpdateDto updateDto) {
        System.out.println("=== UPDATE PROFILE DEBUG ===");
        
        // Extract token from cookies
        String accessToken = null;
        if (request.getCookies() != null) {
            System.out.println("Cookies found: " + request.getCookies().length);
            for (var cookie : request.getCookies()) {
                System.out.println("Cookie: " + cookie.getName() + " = " + cookie.getValue());
                if ("access_token".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                    break;
                }
            }
        } else {
            System.out.println("No cookies found");
        }
        
        System.out.println("Access token: " + accessToken);
        
        if (accessToken == null || !accessToken.startsWith("local-access-")) {
            System.out.println("Invalid or missing token");
            return ResponseEntity.status(401).build();
        }
        
        // Token format: "local-access-email-timestamp"
        // Remove "local-access-" prefix and split by "-"
        String tokenPart = accessToken.substring("local-access-".length());
        String[] parts = tokenPart.split("-");
        
        System.out.println("Token parts: " + java.util.Arrays.toString(parts));
        
        // Find the email part (everything before the last timestamp)
        if (parts.length < 2) {
            System.out.println("Token has insufficient parts");
            return ResponseEntity.status(401).build();
        }
        
        // Join all parts except the last one (timestamp) to reconstruct the email
        StringBuilder emailBuilder = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) emailBuilder.append("-");
            emailBuilder.append(parts[i]);
        }
        String email = emailBuilder.toString();
        
        System.out.println("Extracted email: " + email);
        
        LocalUser user = users.get(email);
        
        if (user == null) {
            System.out.println("User not found for email: " + email);
            return ResponseEntity.status(401).build();
        }
        
        System.out.println("User found: " + user.firstName + " " + user.lastName);
        System.out.println("Updating to: " + updateDto.getFirstName() + " " + updateDto.getLastName());
        
        // Update user profile
        users.put(email, new LocalUser(
            email,
            user.password,
            updateDto.getFirstName(),
            updateDto.getLastName()
        ));
        
        System.out.println("Profile updated successfully");
        return ResponseEntity.ok().build();
    }

    // Simple local user class
    private static class LocalUser {
        final String email;
        final String password;
        final String firstName;
        final String lastName;
        
        LocalUser(String email, String password, String firstName, String lastName) {
            this.email = email;
            this.password = password;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }
}

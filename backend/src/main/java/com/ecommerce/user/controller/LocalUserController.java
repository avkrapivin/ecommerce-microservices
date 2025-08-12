package com.ecommerce.user.controller;

import com.ecommerce.user.dto.UserLoginDto;
import com.ecommerce.user.dto.UserProfileDto;
import com.ecommerce.user.dto.UserRegistrationDto;
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
        String email = accessToken.substring("local-access-".length()).split("-")[0];
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

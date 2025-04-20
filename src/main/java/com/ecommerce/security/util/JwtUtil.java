package com.ecommerce.security.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    public String extractCognitoIdFromToken(String token) {
        try {
            // Remove "Bearer " prefix if it exists
            String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
            
            DecodedJWT decodedJWT = JWT.decode(jwtToken);
            return decodedJWT.getSubject(); // cognitoId находится в subject claim
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract cognitoId from token", e);
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String cognitoId = extractCognitoIdFromToken(token);
            return cognitoId.equals(userDetails.getUsername());
        } catch (Exception e) {
            return false;
        }
    }
} 
package com.ecommerce.user.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SecretHashCalculator {
    private final String clientId;
    private final String clientSecret;

    public SecretHashCalculator(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String calculateSecretHash(String username) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(
                clientSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            mac.update(username.getBytes(StandardCharsets.UTF_8));
            mac.update(clientId.getBytes(StandardCharsets.UTF_8));
            byte[] rawHmac = mac.doFinal();

            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate secret hash: " + e.getMessage(), e);
        }
    }
} 
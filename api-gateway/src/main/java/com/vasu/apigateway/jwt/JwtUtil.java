package com.vasu.apigateway.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT utility for the API Gateway.
 * Handles user token validation and service-to-service token generation/validation.
 * This replaces the shared common module's CommonJwtUtil.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private static final long SERVICE_TOKEN_TTL_MS = 60000; // 1 minute

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validates a user-facing JWT token and returns the claims.
     */
    public Claims validateUserToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates a service-to-service JWT token and returns the service name if valid.
     */
    public String validateServiceToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String type = claims.get("type", String.class);
            if ("service-to-service".equals(type)) {
                return claims.get("service", String.class);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generates a short-lived service-to-service JWT token.
     */
    public String generateServiceToken(String serviceName) {
        return Jwts.builder()
                .subject(serviceName)
                .claim("service", serviceName)
                .claim("type", "service-to-service")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + SERVICE_TOKEN_TTL_MS))
                .signWith(getSigningKey())
                .compact();
    }
}

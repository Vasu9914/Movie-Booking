package com.vasu.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for JWT token parsing and validation.
 */
@Component
public class JwtUtil {

    /**
     * Parse and validate a JWT token, returning the claims.
     *
     * @param token  the JWT token string
     * @param secret the signing secret
     * @return the parsed claims
     * @throws Exception if the token is invalid or expired
     */
    public Claims parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate a JWT token without returning claims.
     *
     * @param token  the JWT token string
     * @param secret the signing secret
     * @return true if the token is valid
     */
    public boolean validateToken(String token, String secret) {
        try {
            parseToken(token, secret);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

package com.vasu.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global JWT authentication filter for the API Gateway.
 *
 * - Validates the Bearer token on every secured request.
 * - Extracts userId and role from the JWT claims.
 * - Propagates them as X-User-Id and X-User-Role headers to downstream services.
 * - Skips authentication for public endpoints (login, register, actuator, Swagger).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Paths that do NOT require authentication.
     */
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/register",
            "/api/auth/login",
            "/actuator/",
            "/swagger-ui/",
            "/v3/api-docs/"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip authentication for public paths
        if (isPublicPath(path)) {
            log.debug("Skipping authentication for public path: {}", path);
            return chain.filter(exchange);
        }

        // Check for Authorization header (reactive API: getHeader() returns List)
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        String authHeader = (authHeaders != null && !authHeaders.isEmpty()) ? authHeaders.get(0) : null;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        // Extract and validate token
        String token = authHeader.substring(7);
        try {
            Claims claims = jwtUtil.parseToken(token, secret);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            if (userId == null || userId.isEmpty()) {
                return unauthorized(exchange, "Invalid token: missing userId");
            }

            // Propagate user info to downstream services via headers
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role != null ? role : "USER")
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        // Also allow GET requests to movie-service endpoints (movies, theatres, screens, shows)
        // These are read-only and should be publicly accessible
        if (path.startsWith("/api/movies/") || path.startsWith("/api/theatres/") ||
            path.startsWith("/api/screens/") || path.startsWith("/api/shows/")) {
            return true;
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        return response.writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))
        ));
    }
}

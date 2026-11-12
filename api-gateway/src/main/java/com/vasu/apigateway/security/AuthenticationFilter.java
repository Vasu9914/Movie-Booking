package com.vasu.apigateway.security;

import com.vasu.apigateway.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
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

import java.util.List;

/**
 * Global authentication filter for the API Gateway.
 * Validates JWT tokens on incoming requests before routing to downstream services.
 * For internal service-to-service calls, the gateway generates and attaches service tokens.
 */
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Value("${gateway.public-paths:/api/auth/**,/actuator/**,/v3/api-docs/**,/swagger-ui/**,/swagger-ui.html}")
    private String publicPathsProperty;

    private List<String> publicPaths;

    public AuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public int getOrder() {
        return -2; // Run before RouteToRequestUrlFilter
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Initialize public paths lazily
        if (publicPaths == null) {
            publicPaths = List.of(publicPathsProperty.split(","));
        }

        // Skip authentication for public paths
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Check for Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange.getResponse());
        }

        String token = authHeader.substring(7);
        try {
            // Validate the user token
            Claims claims = jwtUtil.validateUserToken(token);
            
            // Extract user info and add to request headers for downstream services
            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-Email", email != null ? email : "")
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            return unauthorized(exchange.getResponse());
        }
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : publicPaths) {
            String pattern = publicPath.trim().replace("/**", "").replace("*", "");
            if (path.startsWith(pattern)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String error = "{\"error\":\"Invalid or missing authentication token\"}";
        return response.writeWith(Mono.just(response.bufferFactory().wrap(error.getBytes())));
    }
}

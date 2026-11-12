package com.vasu.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * API Gateway configuration.
 * Defines routes to downstream microservices and configures security.
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service routes
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://localhost:8081"))
                
                // Movie Service routes
                .route("movie-service", r -> r
                        .path("/api/movies/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://localhost:8082"))
                
                // Booking Service routes
                .route("booking-service", r -> r
                        .path("/api/bookings/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://localhost:8083"))
                
                // Payment Service routes
                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://localhost:8084"))
                
                // Notification Service routes
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://localhost:8085"))
                
                // Review Service routes
                .route("review-service", r -> r
                        .path("/api/reviews/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://localhost:8086"))
                .build();
    }

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchange -> exchange
                        .anyExchange().permitAll() // Authentication is handled by AuthenticationFilter
                );
        return http.build();
    }
}

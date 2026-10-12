package com.vasu.userservice.config;

import com.vasu.userservice.entity.User;
import com.vasu.userservice.enums.UserStatus;
import com.vasu.userservice.enums.UserRole;
import com.vasu.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class DevDataSeederConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedUsers() {
        return args -> {
            log.info("=== Seeding User Service development data ===");

            seedUser("admin@movieticket.com", "Admin User", UserStatus.ACTIVE, UserRole.ADMIN);

            log.info("=== User Service seeding complete ===");
        };
    }

    private void seedUser(String email, String name, UserStatus status, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            log.info("Skipping existing user: {}", email);
            return;
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .role(role)
                .status(status)
                .build();

        userRepository.save(user);
        log.info("Seeded user: {} ({})", name, email);
    }
}

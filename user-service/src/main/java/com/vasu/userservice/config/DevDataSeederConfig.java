package com.vasu.userservice.config;

import com.vasu.userservice.entity.User;
import com.vasu.userservice.enums.UserStatus;
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

            seedUser("admin", "admin@movieticket.com", "Admin User", UserStatus.ACTIVE);
            seedUser("user1", "user1@movieticket.com", "John Doe", UserStatus.ACTIVE);
            seedUser("user2", "user2@movieticket.com", "Jane Smith", UserStatus.ACTIVE);
            seedUser("suspended", "suspended@movieticket.com", "Suspended User", UserStatus.SUSPENDED);

            log.info("=== User Service seeding complete ===");
        };
    }

    private void seedUser(String username, String email, String name, UserStatus status) {
        if (userRepository.existsByEmail(email)) {
            log.info("Skipping existing user: {}", email);
            return;
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .status(status)
                .build();

        userRepository.save(user);
        log.info("Seeded user: {} ({})", name, email);
    }
}

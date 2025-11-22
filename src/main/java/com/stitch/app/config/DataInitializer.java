package com.stitch.app.config;

import com.stitch.app.entity.User;
import com.stitch.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initializeAdminUser() {
        return args -> {
            // Check if admin user already exists
            if (userRepository.findByEmail("admin@stitch.com").isEmpty()) {
                User admin = User.builder()
                        .email("admin@stitch.com")
                        .password(passwordEncoder.encode("admin123"))  // Change this password!
                        .fullName("System Administrator")
                        .phoneNumber("0000000000")
                        .role(User.Role.ADMIN)
                        .isActive(true)
                        .build();

                userRepository.save(admin);
                log.info("✓ Admin user created successfully!");
                log.info("  Email: admin@stitch.com");
                log.info("  Password: admin123");
                log.warn("⚠ Please change the admin password after first login!");
            } else {
                log.info("✓ Admin user already exists");
            }
        };
    }
}


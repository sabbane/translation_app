package com.translationapp.config;

import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            // Test Admin
            userRepository.save(User.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .build());

            // Test User
            userRepository.save(User.builder()
                    .username("user")
                    .passwordHash(passwordEncoder.encode("user123"))
                    .role(Role.USER)
                    .build());

            // Test Reviewer
            userRepository.save(User.builder()
                    .username("reviewer")
                    .passwordHash(passwordEncoder.encode("reviewer123"))
                    .role(Role.REVIEWER)
                    .build());

            System.out.println("Test users initialized: admin/admin123, user/user123, reviewer/reviewer123");
        }
    }
}

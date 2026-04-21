package com.translationapp.config;

import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private com.translationapp.repository.UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.translationapp.repository.DocumentRepository documentRepository;

    @Override
    public void run(String... args) throws Exception {
        // Clear and re-seed for consistent test environment
        documentRepository.deleteAll();
        userRepository.deleteAll();
        
        // Test Admin
        saveUser("admin", "admin123", Role.ADMIN);

        // Test Users
        saveUser("user", "user123", Role.USER);
        User u1 = saveUser("user-1", "user123", Role.USER);
        saveUser("user-2", "user123", Role.USER);

        // Test Reviewers
        User r1 = saveUser("reviewer-1", "reviewer123", Role.REVIEWER);
        User r2 = saveUser("reviewer-2", "reviewer123", Role.REVIEWER);
        User r3 = saveUser("reviewer-3", "reviewer123", Role.REVIEWER);

        // Seed enough docs for u1 to pass dashboard tests (needs > 5)
        for (int i = 1; i <= 6; i++) {
            com.translationapp.model.Document d = new com.translationapp.model.Document();
            d.setTitle("Test Doc " + i);
            d.setOriginalText("Content " + i);
            d.setSourceLanguage("EN");
            d.setTargetLanguage(i % 2 == 0 ? "DE" : "FR");
            d.setStatus(i % 3 == 0 ? com.translationapp.model.DocumentStatus.OFFEN : com.translationapp.model.DocumentStatus.KORREKTUR);
            d.setCreator(u1);
            if (d.getStatus() != com.translationapp.model.DocumentStatus.OFFEN) {
                d.setReviewer(r1);
            }
            documentRepository.save(d);
        }

        System.out.println("Test users and 6 documents initialized for E2E tests.");
    }

    private User saveUser(String username, String password, Role role) {
        return userRepository.save(User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .build());
    }
}

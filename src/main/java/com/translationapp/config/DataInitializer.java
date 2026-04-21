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

    @Autowired
    private org.springframework.core.env.Environment env;

    @Override
    public void run(String... args) throws Exception {
        boolean isTestProfile = java.util.Arrays.asList(env.getActiveProfiles()).contains("test");

        if (isTestProfile) {
            System.out.println("TEST PROFILE ACTIVE: Clearing and re-seeding test database...");
            documentRepository.deleteAll();
            userRepository.deleteAll();
        }

        // 1. Seed Users (either always ensure or full seed if test)
        ensureUserExists("admin", "admin123", Role.ADMIN);
        ensureUserExists("user", "user123", Role.USER);
        User u1 = ensureUserExists("user-1", "user123", Role.USER);
        ensureUserExists("user-2", "user123", Role.USER);
        User r1 = ensureUserExists("reviewer-1", "reviewer123", Role.REVIEWER);
        ensureUserExists("reviewer-2", "reviewer123", Role.REVIEWER);
        ensureUserExists("reviewer-3", "reviewer123", Role.REVIEWER);

        // 2. Seed Docs (Full seed if test, otherwise only if empty)
        if (isTestProfile || documentRepository.count() == 0) {
            seedTestDocuments(u1, r1);
        }

        System.out.println(isTestProfile ? "Test database seeded." : "Base test data ensured.");
    }

    private void seedTestDocuments(User creator, User reviewer) {
        for (int i = 1; i <= 6; i++) {
            com.translationapp.model.Document d = new com.translationapp.model.Document();
            d.setTitle("Test Doc " + i);
            d.setOriginalText("Content " + i);
            d.setSourceLanguage("EN");
            d.setTargetLanguage(i % 2 == 0 ? "DE" : "FR");
            d.setStatus(i % 3 == 0 ? com.translationapp.model.DocumentStatus.OFFEN : com.translationapp.model.DocumentStatus.KORREKTUR);
            d.setCreator(creator);
            if (d.getStatus() != com.translationapp.model.DocumentStatus.OFFEN) {
                d.setReviewer(reviewer);
            }
            documentRepository.save(d);
        }
    }

    private User ensureUserExists(String username, String password, Role role) {
        return userRepository.findByUsername(username).orElseGet(() -> saveUser(username, password, role));
    }

    private User saveUser(String username, String password, Role role) {
        return userRepository.save(User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .build());
    }
}

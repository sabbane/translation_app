package com.translationapp.controller;

import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import com.translationapp.repository.DocumentRepository;
import com.translationapp.model.Document;
import com.translationapp.security.services.UserDetailsImpl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DocumentRepository documentRepository;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, DocumentRepository documentRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.documentRepository = documentRepository;
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(u -> new UserDto(u.getId(), u.getUsername(), u.getRole().name()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserCreateRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: Username is already taken!"));
        }

        Role role = Role.USER;
        try {
            if (request.getRole() != null) {
                role = Role.valueOf(request.getRole().toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: Invalid role"));
        }

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);

        return ResponseEntity.ok(new UserDto(user.getId(), user.getUsername(), user.getRole().name()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Error: Username is already taken!"));
            }
            user.setUsername(request.getUsername());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            try {
                user.setRole(Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Error: Invalid role"));
            }
        }

        userRepository.save(user);

        return ResponseEntity.ok(new UserDto(user.getId(), user.getUsername(), user.getRole().name()));
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        // Prevent self-deletion
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (currentUser.getId().equals(user.getId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Error: You cannot delete your own account!"));
        }

        // Clean up documents using optimized queries
        documentRepository.clearReviewer(user);
        documentRepository.deleteByCreator(user);

        userRepository.delete(user);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    // DTOs
    public static class UserDto {
        private UUID id;
        private String username;
        private String role;

        public UserDto(UUID id, String username, String role) {
            this.id = id;
            this.username = username;
            this.role = role;
        }

        public UUID getId() { return id; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }

    public static class UserCreateRequest {
        private String username;
        private String password;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class UserUpdateRequest {
        private String username;
        private String password;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}

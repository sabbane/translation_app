package com.translationapp.service;

import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.DocumentRepository;
import com.translationapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DocumentRepository documentRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, DocumentRepository documentRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.documentRepository = documentRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User createUser(String username, String password, String roleName) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Username is already taken!");
        }

        Role role = Role.USER;
        if (roleName != null) {
            try {
                role = Role.valueOf(roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Invalid role");
            }
        }

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .build();

        return userRepository.save(user);
    }

    public User updateUser(UUID id, String username, String password, String roleName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (username != null && !username.equals(user.getUsername())) {
            if (userRepository.findByUsername(username).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Username is already taken!");
            }
            user.setUsername(username);
        }

        if (password != null && !password.isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }

        if (roleName != null) {
            try {
                user.setRole(Role.valueOf(roleName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: Invalid role");
            }
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID id, UUID currentUserId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        if (currentUserId.equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error: You cannot delete your own account!");
        }

        // Clean up documents
        documentRepository.clearReviewer(user);
        documentRepository.deleteByCreator(user);

        userRepository.delete(user);
    }
}

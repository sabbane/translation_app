package com.translationapp.controller;

import com.translationapp.model.User;
import com.translationapp.repository.UserRepository;
import com.translationapp.security.services.UserDetailsImpl;
import com.translationapp.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers().stream()
                .map(u -> new UserDto(u.getId(), u.getUsername(), u.getRole().name()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserCreateRequest request) {
        User user = userService.createUser(request.getUsername(), request.getPassword(), request.getRole());
        return ResponseEntity.ok(new UserDto(user.getId(), user.getUsername(), user.getRole().name()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @RequestBody UserUpdateRequest request) {
        User user = userService.updateUser(id, request.getUsername(), request.getPassword(), request.getRole());
        return ResponseEntity.ok(new UserDto(user.getId(), user.getUsername(), user.getRole().name()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        UserDetailsImpl currentUser = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.deleteUser(id, currentUser.getId());
        return ResponseEntity.ok().build();
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


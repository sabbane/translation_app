package com.translationapp.service;

import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.DocumentRepository;
import com.translationapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private UserService userService;

    private User admin;
    private User user;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .role(Role.ADMIN)
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .username("user")
                .role(Role.USER)
                .build();
    }

    @Test
    void createUser_ShouldHashPassword() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainPassword")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User created = userService.createUser("newuser", "plainPassword", "USER");

        assertEquals("hashedPassword", created.getPasswordHash());
        verify(passwordEncoder).encode("plainPassword");
    }

    @Test
    void createUser_DuplicateUsername_ShouldThrowException() {
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(user));

        assertThrows(ResponseStatusException.class, () -> {
            userService.createUser("existing", "password", "USER");
        });
    }

    @Test
    void deleteUser_SelfDeletion_ShouldThrowException() {
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThrows(ResponseStatusException.class, () -> {
            userService.deleteUser(admin.getId(), admin.getId());
        });
    }

    @Test
    void deleteUser_ShouldCleanUpDocuments() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.deleteUser(user.getId(), admin.getId());

        verify(documentRepository).clearReviewer(user);
        verify(documentRepository).deleteByCreator(user);
        verify(userRepository).delete(user);
    }
}

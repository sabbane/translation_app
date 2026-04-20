package com.translationapp.controller;

import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.UserRepository;
import com.translationapp.security.services.UserDetailsImpl;
import com.translationapp.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserController userController;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .role(Role.ADMIN)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(User user) {
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        UserDetailsImpl userDetails = new UserDetailsImpl(user.getId(), user.getUsername(), "hash", authorities);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void testGetAllUsers() {
        when(userService.getAllUsers()).thenReturn(Collections.singletonList(adminUser));

        ResponseEntity<List<UserController.UserDto>> response = userController.getAllUsers();
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(userService).getAllUsers();
    }

    @Test
    void testCreateUser() {
        UserController.UserCreateRequest request = new UserController.UserCreateRequest();
        request.setUsername("newUser");
        request.setPassword("password");
        request.setRole("USER");

        User user = User.builder().id(UUID.randomUUID()).username("newUser").role(Role.USER).build();
        when(userService.createUser(any(), any(), any())).thenReturn(user);

        ResponseEntity<?> response = userController.createUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService).createUser("newUser", "password", "USER");
    }

    @Test
    void testDeleteUser() {
        mockSecurityContext(adminUser);
        UUID targetId = UUID.randomUUID();

        ResponseEntity<?> response = userController.deleteUser(targetId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService).deleteUser(targetId, adminUser.getId());
    }
}


package com.translationapp;

import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User normalUser;
    private String adminJwt;
    private String userJwt;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Create test users
        adminUser = new User("admin_test", passwordEncoder.encode("password"), Role.ADMIN);
        userRepository.save(adminUser);

        normalUser = new User("user_test", passwordEncoder.encode("password"), Role.USER);
        userRepository.save(normalUser);

        // Login to get JWT tokens
        adminJwt = loginAndGetToken("admin_test", "password");
        userJwt = loginAndGetToken("user_test", "password");
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private String loginAndGetToken(String username, String password) {
        Map<String, String> loginRequest = Map.of(
                "username", username,
                "password", password
        );
        ResponseEntity<Map> response = restTemplate.postForEntity(getBaseUrl() + "/auth/signin", loginRequest, Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return (String) response.getBody().get("accessToken");
    }

    private HttpHeaders getHeaders(String jwt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwt);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void testAdminCanGetAllUsers() {
        HttpEntity<Void> request = new HttpEntity<>(getHeaders(adminJwt));
        ResponseEntity<List> response = restTemplate.exchange(
                getBaseUrl() + "/users",
                HttpMethod.GET,
                request,
                List.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size(), "Should return admin_test and user_test");
    }

    @Test
    void testUserCannotAccessUserManagement() {
        HttpEntity<Void> request = new HttpEntity<>(getHeaders(userJwt));
        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/users",
                HttpMethod.GET,
                request,
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Regular users should not access user management");
    }

    @Test
    void testAdminCanCreateUser() {
        Map<String, String> newUserRequest = Map.of(
                "username", "new_reviewer",
                "password", "secret123",
                "role", "REVIEWER"
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(newUserRequest, getHeaders(adminJwt));
        ResponseEntity<Map> response = restTemplate.postForEntity(getBaseUrl() + "/users", request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("new_reviewer", response.getBody().get("username"));
        assertEquals("REVIEWER", response.getBody().get("role"));

        // Verify in DB
        assertTrue(userRepository.findByUsername("new_reviewer").isPresent());
        User savedUser = userRepository.findByUsername("new_reviewer").get();
        assertEquals(Role.REVIEWER, savedUser.getRole());
    }

    @Test
    void testAdminCannotCreateUserWithExistingUsername() {
        Map<String, String> newUserRequest = Map.of(
                "username", "user_test", // Already exists
                "password", "secret123",
                "role", "USER"
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(newUserRequest, getHeaders(adminJwt));
        ResponseEntity<Map> response = restTemplate.postForEntity(getBaseUrl() + "/users", request, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("already taken"));
    }

    @Test
    void testAdminCanUpdateUser() {
        Map<String, String> updateRequest = Map.of(
                "role", "REVIEWER"
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(updateRequest, getHeaders(adminJwt));
        ResponseEntity<Map> response = restTemplate.exchange(
                getBaseUrl() + "/users/" + normalUser.getId(),
                HttpMethod.PUT,
                request,
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify in DB
        User updatedUser = userRepository.findById(normalUser.getId()).get();
        assertEquals(Role.REVIEWER, updatedUser.getRole());
    }

    @Test
    void testAdminCannotDeleteSelf() {
        HttpEntity<Void> request = new HttpEntity<>(getHeaders(adminJwt));
        ResponseEntity<Map> response = restTemplate.exchange(
                getBaseUrl() + "/users/" + adminUser.getId(),
                HttpMethod.DELETE,
                request,
                Map.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("cannot delete your own account"));
        
        // Verify admin still exists in DB
        assertTrue(userRepository.findById(adminUser.getId()).isPresent());
    }
}

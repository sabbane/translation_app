package com.translationapp;

import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.UserRepository;
import com.translationapp.service.DeepLService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {BackendApplication.class, TranslationIntegrationTest.TestConfig.class})
@ActiveProfiles("test")
public class TranslationIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public DeepLService deepLService(RestTemplate restTemplate) {
            return new DeepLService(restTemplate) {
                @Override
                public String translate(String text, String sourceLang, String targetLang) {
                    if ("Hello World".equals(text) && "EN".equals(sourceLang) && "DE".equals(targetLang)) {
                        return "Hallo Welt";
                    }
                    return "Error";
                }
            };
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User normalUser;
    private String userJwt;

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api";
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Create test user
        normalUser = new User("user_test", passwordEncoder.encode("password"), Role.USER);
        userRepository.save(normalUser);

        // Login to get JWT
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
        if (jwt != null) {
            headers.setBearerAuth(jwt);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void testUserCanTranslate() {
        Map<String, String> translationRequest = Map.of(
                "text", "Hello World",
                "sourceLang", "EN",
                "targetLang", "DE"
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(translationRequest, getHeaders(userJwt));
        ResponseEntity<Map> response = restTemplate.postForEntity(getBaseUrl() + "/translation/auto", request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Hallo Welt", response.getBody().get("translatedText"));
    }

    @Test
    void testUnauthorizedCannotTranslate() {
        Map<String, String> translationRequest = Map.of(
                "text", "Hello World",
                "sourceLang", "EN",
                "targetLang", "DE"
        );

        // Request without JWT token
        HttpEntity<Map<String, String>> request = new HttpEntity<>(translationRequest, getHeaders(null));
        ResponseEntity<Map> response = restTemplate.postForEntity(getBaseUrl() + "/translation/auto", request, Map.class);

        // Should be rejected by Spring Security
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}

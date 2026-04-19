package com.translationapp;

import com.translationapp.controller.UserController;
import com.translationapp.model.Document;
import com.translationapp.model.DocumentStatus;
import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.DocumentRepository;
import com.translationapp.repository.UserRepository;
import com.translationapp.security.jwt.JwtUtils;
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

import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DocumentIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

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
        // Clean up database before tests
        documentRepository.deleteAll();
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
        documentRepository.deleteAll();
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
    void testUserCanCreateDocument() {
        // 1. User creates a document
        Document newDoc = new Document();
        newDoc.setTitle("Integration Test Doc");
        newDoc.setOriginalText("Hello World");

        HttpEntity<Document> request = new HttpEntity<>(newDoc, getHeaders(userJwt));
        ResponseEntity<Document> response = restTemplate.postForEntity(getBaseUrl() + "/documents", request, Document.class);

        // Verify response
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());
        assertEquals("Integration Test Doc", response.getBody().getTitle());
        assertEquals(DocumentStatus.OFFEN, response.getBody().getStatus());

        // Verify in DB
        List<Document> documentsInDb = documentRepository.findAll();
        assertEquals(1, documentsInDb.size());
        assertEquals("Integration Test Doc", documentsInDb.get(0).getTitle());
        assertEquals(normalUser.getId(), documentsInDb.get(0).getCreator().getId());
    }

    @Test
    void testAdminCannotCreateDocument() {
        Document newDoc = new Document();
        newDoc.setTitle("Admin Test Doc");

        HttpEntity<Document> request = new HttpEntity<>(newDoc, getHeaders(adminJwt));
        ResponseEntity<String> response = restTemplate.postForEntity(getBaseUrl() + "/documents", request, String.class);

        // Admin should be forbidden (403) from creating documents
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        // Verify DB is still empty
        assertEquals(0, documentRepository.findAll().size());
    }

    @Test
    void testAdminCanFetchAllDocuments() {
        // Create a document directly in DB first
        Document doc = new Document();
        doc.setTitle("Admin Fetch Test");
        doc.setOriginalText("Test content");
        doc.setCreator(normalUser);
        doc.setStatus(DocumentStatus.OFFEN);
        documentRepository.save(doc);

        // Admin fetches documents
        HttpEntity<Void> request = new HttpEntity<>(getHeaders(adminJwt));
        ResponseEntity<Map> response = restTemplate.exchange(
                getBaseUrl() + "/documents",
                HttpMethod.GET,
                request,
                Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> content = (List<?>) response.getBody().get("content");
        assertEquals(1, content.size());
    }

    @Test
    void testUserDeletionAlsoDeletesCreatedDocuments() {
        // 1. Create a user
        User temporaryUser = new User("temp_user", passwordEncoder.encode("password"), Role.USER);
        userRepository.save(temporaryUser);

        // 2. Create a document by this user
        Document doc = new Document();
        doc.setTitle("To Be Deleted");
        doc.setCreator(temporaryUser);
        doc.setStatus(DocumentStatus.OFFEN);
        documentRepository.save(doc);

        assertEquals(1, documentRepository.findAll().size());

        // 3. Admin deletes the user
        HttpEntity<Void> request = new HttpEntity<>(getHeaders(adminJwt));
        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/users/" + temporaryUser.getId(),
                HttpMethod.DELETE,
                request,
                String.class
        );

        // 4. Verify deletion
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // The user should be gone
        assertTrue(userRepository.findById(temporaryUser.getId()).isEmpty());
        
        // The document should also be gone
        assertTrue(documentRepository.findAll().isEmpty(), "Documents created by deleted user should be deleted");
    }
}

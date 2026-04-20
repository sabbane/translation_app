package com.translationapp.controller;

import com.translationapp.model.Document;
import com.translationapp.model.DocumentStatus;
import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.UserRepository;
import com.translationapp.security.services.UserDetailsImpl;
import com.translationapp.service.DocumentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
class DocumentControllerTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DocumentController documentController;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .role(Role.ADMIN)
                .build();

        regularUser = User.builder()
                .id(UUID.randomUUID())
                .username("user")
                .role(Role.USER)
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
        
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    void testGetDashboard() {
        mockSecurityContext(regularUser);
        Page<Document> page = new PageImpl<>(Collections.emptyList());
        when(documentService.getDocumentsForUser(any(User.class), any())).thenReturn(page);

        ResponseEntity<Page<Document>> response = documentController.getDashboard(0, 10, new String[]{"createdAt", "desc"});
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(documentService).getDocumentsForUser(eq(regularUser), any());
    }

    @Test
    void testCreateDocument() {
        mockSecurityContext(regularUser);
        Document doc = new Document();
        when(documentService.createDocument(any(Document.class), any(User.class))).thenReturn(doc);

        ResponseEntity<Document> response = documentController.createDocument(doc);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(documentService).createDocument(doc, regularUser);
    }

    @Test
    void testDeleteDocument() {
        mockSecurityContext(regularUser);
        UUID docId = UUID.randomUUID();

        ResponseEntity<?> response = documentController.deleteDocument(docId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(documentService).deleteDocument(docId, regularUser);
    }
}


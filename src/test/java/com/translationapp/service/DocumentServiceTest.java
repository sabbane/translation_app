package com.translationapp.service;

import com.translationapp.model.Document;
import com.translationapp.model.DocumentStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DocumentService documentService;

    private User user;
    private User reviewer;
    private User otherUser;
    private Document document;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("user")
                .role(Role.USER)
                .build();

        reviewer = User.builder()
                .id(UUID.randomUUID())
                .username("reviewer")
                .role(Role.REVIEWER)
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .username("other")
                .role(Role.USER)
                .build();

        document = new Document();
        document.setId(UUID.randomUUID());
        document.setTitle("Test Doc");
        document.setCreator(user);
        document.setStatus(DocumentStatus.OFFEN);
    }

    @Test
    void createDocument_ShouldSetDefaultValues() {
        Document newDoc = new Document();
        newDoc.setOriginalText("Hello");

        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArguments()[0]);

        Document created = documentService.createDocument(newDoc, user);

        assertEquals(user, created.getCreator());
        assertEquals(DocumentStatus.OFFEN, created.getStatus());
        assertEquals("Unbenanntes Dokument", created.getTitle());
        verify(documentRepository).save(newDoc);
    }

    @Test
    void getDocumentById_AsCreator_ShouldSucceed() {
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        Document found = documentService.getDocumentById(document.getId(), user);

        assertEquals(document, found);
    }

    @Test
    void getDocumentById_AsOtherUser_ShouldThrowForbidden() {
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        assertThrows(ResponseStatusException.class, () -> {
            documentService.getDocumentById(document.getId(), otherUser);
        });
    }

    @Test
    void updateDocument_WhenInReview_ShouldThrowForbiddenForUser() {
        document.setStatus(DocumentStatus.IN_PRUEFUNG);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        Document updateDetails = new Document();
        updateDetails.setTitle("New Title");

        assertThrows(ResponseStatusException.class, () -> {
            documentService.updateDocument(document.getId(), updateDetails, user);
        });
    }

    @Test
    void updateStatus_AsAssignedReviewer_ShouldSucceed() {
        document.setReviewer(reviewer);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArguments()[0]);

        Document updated = documentService.updateStatus(document.getId(), DocumentStatus.ERLEDIGT, reviewer);

        assertEquals(DocumentStatus.ERLEDIGT, updated.getStatus());
        verify(documentRepository).save(document);
    }

    @Test
    void updateStatus_AsUnassignedReviewer_ShouldThrowForbidden() {
        User otherReviewer = User.builder().id(UUID.randomUUID()).role(Role.REVIEWER).build();
        document.setReviewer(reviewer);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        assertThrows(ResponseStatusException.class, () -> {
            documentService.updateStatus(document.getId(), DocumentStatus.ERLEDIGT, otherReviewer);
        });
    }

    @Test
    void deleteDocument_AsCreator_ShouldSucceed() {
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        documentService.deleteDocument(document.getId(), user);

        verify(documentRepository).delete(document);
    }
}

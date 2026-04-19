package com.translationapp.controller;

import com.translationapp.model.Document;
import com.translationapp.model.DocumentStatus;
import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.DocumentRepository;
import com.translationapp.repository.UserRepository;
import com.translationapp.security.services.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DocumentControllerTest {

    private FakeDocumentRepository documentRepository;
    private FakeUserRepository userRepository;
    private DocumentController documentController;

    private User adminUser;
    private User regularUser;
    private User reviewerUser;

    @BeforeEach
    void setUp() {
        documentRepository = new FakeDocumentRepository();
        userRepository = new FakeUserRepository();
        documentController = new DocumentController(documentRepository, userRepository);

        adminUser = new User("admin", "hash", Role.ADMIN);
        adminUser.setId(UUID.randomUUID());
        userRepository.save(adminUser);

        regularUser = new User("user", "hash", Role.USER);
        regularUser.setId(UUID.randomUUID());
        userRepository.save(regularUser);

        reviewerUser = new User("reviewer", "hash", Role.REVIEWER);
        reviewerUser.setId(UUID.randomUUID());
        userRepository.save(reviewerUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockSecurityContext(User user) {
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        UserDetailsImpl userDetails = new UserDetailsImpl(user.getId(), user.getUsername(), user.getPasswordHash(), authorities);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void testGetDashboard_AdminGetsAll() {
        mockSecurityContext(adminUser);

        Document doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setTitle("Test Doc");
        doc.setCreator(regularUser);
        documentRepository.save(doc);

        ResponseEntity<Page<Document>> response = documentController.getDashboard(0, 10, new String[]{"createdAt", "desc"});
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, Objects.requireNonNull(response.getBody()).getContent().size());
    }

    @Test
    void testGetDashboard_UserGetsOnlyOwn() {
        mockSecurityContext(regularUser);

        Document docOwn = new Document();
        docOwn.setId(UUID.randomUUID());
        docOwn.setTitle("Own Doc");
        docOwn.setCreator(regularUser);
        documentRepository.save(docOwn);

        Document docOther = new Document();
        docOther.setId(UUID.randomUUID());
        docOther.setTitle("Other Doc");
        docOther.setCreator(reviewerUser); // someone else
        documentRepository.save(docOther);

        ResponseEntity<Page<Document>> response = documentController.getDashboard(0, 10, new String[]{"createdAt", "desc"});
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, Objects.requireNonNull(response.getBody()).getContent().size());
        assertEquals("Own Doc", response.getBody().getContent().get(0).getTitle());
    }

    @Test
    void testCreateDocument() {
        mockSecurityContext(regularUser);

        Document doc = new Document();
        doc.setTitle("New Translation");
        doc.setOriginalText("Hello");

        ResponseEntity<Document> response = documentController.createDocument(doc);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(Objects.requireNonNull(response.getBody()).getId());
        assertEquals(DocumentStatus.OFFEN, response.getBody().getStatus());
        assertEquals(regularUser.getId(), response.getBody().getCreator().getId());
    }

    @Test
    void testUpdateDocument_AdminDenied() {
        mockSecurityContext(adminUser);

        Document doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setTitle("Test Doc");
        doc.setCreator(regularUser);
        documentRepository.save(doc);

        Document updateDetails = new Document();
        updateDetails.setTitle("Updated Title");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            documentController.updateDocument(doc.getId(), updateDetails);
        });

        assertEquals(403, exception.getStatusCode().value());
        assertTrue(Objects.requireNonNull(exception.getReason()).contains("read-only"));
    }

    @Test
    void testUpdateDocument_CreatorAllowed() {
        mockSecurityContext(regularUser);

        Document doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setTitle("Test Doc");
        doc.setCreator(regularUser);
        doc.setStatus(DocumentStatus.OFFEN);
        documentRepository.save(doc);

        Document updateDetails = new Document();
        updateDetails.setTitle("Updated Title");
        updateDetails.setOriginalText("New text");

        ResponseEntity<Document> response = documentController.updateDocument(doc.getId(), updateDetails);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Updated Title", Objects.requireNonNull(response.getBody()).getTitle());
    }

    @Test
    void testDeleteDocument_AdminAllowed() {
        mockSecurityContext(adminUser);

        Document doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setTitle("Test Doc");
        doc.setCreator(regularUser);
        documentRepository.save(doc);

        ResponseEntity<?> response = documentController.deleteDocument(doc.getId());

        assertEquals(200, response.getStatusCode().value());
        assertTrue(documentRepository.findAll().isEmpty());
    }

    // Fake Implementations

    private static class FakeUserRepository implements UserRepository {
        private final List<User> users = new ArrayList<>();

        @Override
        public List<User> findAll() { return users; }

        @Override
        public Optional<User> findByUsername(String username) {
            return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
        }

        @Override
        public <S extends User> S save(S entity) {
            users.removeIf(u -> u.getId().equals(entity.getId()));
            users.add(entity);
            return entity;
        }

        @Override
        public Optional<User> findById(UUID id) {
            return users.stream().filter(u -> u.getId().equals(id)).findFirst();
        }

        @Override
        public void delete(User entity) {
            users.removeIf(u -> u.getId().equals(entity.getId()));
        }

        @Override public List<User> findAllByRole(Role role) { return null; }
        @Override public void flush() {}
        @Override public <S extends User> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<User> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<UUID> uuids) {}
        @Override public void deleteAllInBatch() {}
        @Override public User getOne(UUID uuid) { return null; }
        @Override public User getById(UUID uuid) { return null; }
        @Override public User getReferenceById(UUID uuid) { return null; }
        @Override public <S extends User> List<S> findAll(org.springframework.data.domain.Example<S> example) { return null; }
        @Override public <S extends User> List<S> findAll(org.springframework.data.domain.Example<S> example, Sort sort) { return null; }
        @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public boolean existsById(UUID uuid) { return false; }
        @Override public List<User> findAllById(Iterable<UUID> uuids) { return null; }
        @Override public long count() { return 0; }
        @Override public void deleteById(UUID uuid) {}
        @Override public void deleteAllById(Iterable<? extends UUID> uuids) {}
        @Override public void deleteAll(Iterable<? extends User> entities) {}
        @Override public void deleteAll() {}
        @Override public List<User> findAll(Sort sort) { return null; }
        @Override public Page<User> findAll(Pageable pageable) { return null; }
        @Override public <S extends User> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends User> Page<S> findAll(org.springframework.data.domain.Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends User> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends User> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends User, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    }

    private static class FakeDocumentRepository implements DocumentRepository {
        private final List<Document> documents = new ArrayList<>();

        @Override
        public Page<Document> findAll(Pageable pageable) {
            return new PageImpl<>(documents, pageable, documents.size());
        }

        @Override
        public Page<Document> findAllByCreator(User creator, Pageable pageable) {
            List<Document> filtered = new ArrayList<>();
            for (Document d : documents) {
                if (d.getCreator().getId().equals(creator.getId())) filtered.add(d);
            }
            return new PageImpl<>(filtered, pageable, filtered.size());
        }

        @Override
        public Page<Document> findAllByReviewer(User reviewer, Pageable pageable) {
            List<Document> filtered = new ArrayList<>();
            for (Document d : documents) {
                if (d.getReviewer() != null && d.getReviewer().getId().equals(reviewer.getId())) filtered.add(d);
            }
            return new PageImpl<>(filtered, pageable, filtered.size());
        }

        @Override
        public List<Document> findAllByCreator(User creator) {
            List<Document> filtered = new ArrayList<>();
            for (Document d : documents) {
                if (d.getCreator().getId().equals(creator.getId())) filtered.add(d);
            }
            return filtered;
        }

        @Override
        public List<Document> findAllByReviewer(User reviewer) {
            List<Document> filtered = new ArrayList<>();
            for (Document d : documents) {
                if (d.getReviewer() != null && d.getReviewer().getId().equals(reviewer.getId())) filtered.add(d);
            }
            return filtered;
        }

        @Override
        public Optional<Document> findById(UUID id) {
            return documents.stream().filter(d -> d.getId().equals(id)).findFirst();
        }

        @Override
        public <S extends Document> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            } else {
                documents.removeIf(d -> d.getId().equals(entity.getId()));
            }
            documents.add(entity);
            return entity;
        }

        @Override
        public void delete(Document entity) {
            documents.removeIf(d -> d.getId().equals(entity.getId()));
        }

        public List<Document> findAll() { return documents; }

        @Override public void clearReviewer(User reviewer) {}
        @Override public void deleteByCreator(User creator) {}
        @Override public void flush() {}
        @Override public <S extends Document> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Document> List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<Document> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<UUID> uuids) {}
        @Override public void deleteAllInBatch() {}
        @Override public Document getOne(UUID uuid) { return null; }
        @Override public Document getById(UUID uuid) { return null; }
        @Override public Document getReferenceById(UUID uuid) { return null; }
        @Override public <S extends Document> List<S> findAll(org.springframework.data.domain.Example<S> example) { return null; }
        @Override public <S extends Document> List<S> findAll(org.springframework.data.domain.Example<S> example, Sort sort) { return null; }
        @Override public <S extends Document> List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public boolean existsById(UUID uuid) { return false; }
        @Override public List<Document> findAllById(Iterable<UUID> uuids) { return null; }
        @Override public long count() { return 0; }
        @Override public void deleteById(UUID uuid) {}
        @Override public void deleteAllById(Iterable<? extends UUID> uuids) {}
        @Override public void deleteAll(Iterable<? extends Document> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Document> findAll(Sort sort) { return null; }
        @Override public <S extends Document> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends Document> Page<S> findAll(org.springframework.data.domain.Example<S> example, Pageable pageable) { return null; }
        @Override public <S extends Document> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends Document> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends Document, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    }
}

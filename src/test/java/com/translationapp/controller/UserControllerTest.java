package com.translationapp.controller;

import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    @Test
    void testGetAllUsers() {
        // Given
        UserRepository fakeRepo = new FakeUserRepository();
        User user1 = new User("admin1", "hash", Role.ADMIN);
        user1.setId(UUID.randomUUID());
        fakeRepo.save(user1);

        UserController controller = new UserController(fakeRepo, new FakePasswordEncoder(), null);

        // When
        ResponseEntity<List<UserController.UserDto>> response = controller.getAllUsers();

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("admin1", response.getBody().get(0).getUsername());
    }

    @Test
    void testCreateUser() {
        // Given
        UserRepository fakeRepo = new FakeUserRepository();
        UserController controller = new UserController(fakeRepo, new FakePasswordEncoder(), null);
        
        UserController.UserCreateRequest request = new UserController.UserCreateRequest();
        request.setUsername("newUser");
        request.setPassword("password123");
        request.setRole("REVIEWER");

        // When
        ResponseEntity<?> response = controller.createUser(request);

        // Then
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, fakeRepo.findAll().size());
        
        User savedUser = fakeRepo.findAll().get(0);
        assertEquals("newUser", savedUser.getUsername());
        assertEquals("encoded_password123", savedUser.getPasswordHash());
        assertEquals(Role.REVIEWER, savedUser.getRole());
    }

    // Fake Implementations for avoiding Mockito / Spring Context overhead in unit tests
    
    private static class FakePasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(CharSequence rawPassword) {
            return "encoded_" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encode(rawPassword).equals(encodedPassword);
        }
    }

    private static class FakeUserRepository implements UserRepository {
        private final java.util.List<User> users = new java.util.ArrayList<>();

        @Override
        public List<User> findAll() { return users; }

        @Override
        public Optional<User> findByUsername(String username) {
            return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
        }

        @Override
        public <S extends User> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            } else {
                users.removeIf(u -> u.getId().equals(entity.getId()));
            }
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

        // Other required methods are stubbed with null or default
        @Override public List<User> findAllByRole(Role role) { return null; }
        @Override public void flush() {}
        @Override public <S extends User> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends User> java.util.List<S> saveAllAndFlush(Iterable<S> entities) { return null; }
        @Override public void deleteAllInBatch(Iterable<User> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<UUID> uuids) {}
        @Override public void deleteAllInBatch() {}
        @Override public User getOne(UUID uuid) { return null; }
        @Override public User getById(UUID uuid) { return null; }
        @Override public User getReferenceById(UUID uuid) { return null; }
        @Override public <S extends User> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example) { return null; }
        @Override public <S extends User> java.util.List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return null; }
        @Override public <S extends User> java.util.List<S> saveAll(Iterable<S> entities) { return null; }
        @Override public boolean existsById(UUID uuid) { return false; }
        @Override public java.util.List<User> findAllById(Iterable<UUID> uuids) { return null; }
        @Override public long count() { return 0; }
        @Override public void deleteById(UUID uuid) {}
        @Override public void deleteAllById(Iterable<? extends UUID> uuids) {}
        @Override public void deleteAll(Iterable<? extends User> entities) {}
        @Override public void deleteAll() {}
        @Override public java.util.List<User> findAll(org.springframework.data.domain.Sort sort) { return null; }
        @Override public org.springframework.data.domain.Page<User> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends User> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends User> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends User> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends User> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends User, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    }
}

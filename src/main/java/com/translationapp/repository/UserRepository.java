package com.translationapp.repository;

import com.translationapp.model.User;
import com.translationapp.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    List<User> findAllByRole(Role role);
}
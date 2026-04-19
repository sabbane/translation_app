package com.translationapp.repository;

import com.translationapp.model.Document;
import com.translationapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    // Für das User-Dashboard: Nur eigene Dokumente
    Page<Document> findAllByCreator(User creator, Pageable pageable);
    List<Document> findAllByCreator(User creator);
    
    // Für das Reviewer-Dashboard: Nur zugewiesene Dokumente
    Page<Document> findAllByReviewer(User reviewer, Pageable pageable);
    List<Document> findAllByReviewer(User reviewer);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM Document d WHERE d.creator = :creator")
    void deleteByCreator(com.translationapp.model.User creator);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE Document d SET d.reviewer = null WHERE d.reviewer = :reviewer")
    void clearReviewer(com.translationapp.model.User reviewer);
}
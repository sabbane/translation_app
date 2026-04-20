package com.translationapp.controller;

import com.translationapp.model.Document;
import com.translationapp.model.DocumentStatus;
import com.translationapp.model.User;
import com.translationapp.repository.UserRepository;
import com.translationapp.security.services.UserDetailsImpl;
import com.translationapp.service.DocumentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository;

    public DocumentController(DocumentService documentService, UserRepository userRepository) {
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @GetMapping
    public ResponseEntity<Page<Document>> getDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        User currentUser = getCurrentUser();
        String sortField = sort[0];
        Sort.Direction sortDirection = sort.length > 1 && sort[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));

        return ResponseEntity.ok(documentService.getDocumentsForUser(currentUser, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Document> createDocument(@RequestBody Document document) {
        User currentUser = getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.createDocument(document, currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable UUID id) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(documentService.getDocumentById(id, currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(@PathVariable UUID id, @RequestBody Document documentDetails) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(documentService.updateDocument(id, documentDetails, currentUser));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Document> assignReviewer(
            @PathVariable UUID id, 
            @RequestParam UUID reviewerId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime deadline) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(documentService.assignReviewer(id, reviewerId, deadline, currentUser));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<Document> updateStatus(@PathVariable UUID id, @RequestParam DocumentStatus status) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(documentService.updateStatus(id, status, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> deleteDocument(@PathVariable UUID id) {
        User currentUser = getCurrentUser();
        documentService.deleteDocument(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/reviewers")
    public ResponseEntity<List<User>> getReviewers() {
        return ResponseEntity.ok(documentService.getReviewers());
    }
}


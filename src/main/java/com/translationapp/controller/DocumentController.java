package com.translationapp.controller;

import com.translationapp.model.Document;
import com.translationapp.model.DocumentStatus;
import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.DocumentRepository;
import com.translationapp.repository.UserRepository;
import com.translationapp.security.services.UserDetailsImpl;
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

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public DocumentController(DocumentRepository documentRepository, UserRepository userRepository) {
        this.documentRepository = documentRepository;
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

        Page<Document> documents;

        if (currentUser.getRole() == Role.ADMIN) {
            documents = documentRepository.findAll(pageable);
        } else if (currentUser.getRole() == Role.REVIEWER) {
            documents = documentRepository.findAllByReviewer(currentUser, pageable);
        } else {
            documents = documentRepository.findAllByCreator(currentUser, pageable);
        }

        return ResponseEntity.ok(documents);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Document> createDocument(@RequestBody Document document) {
        User currentUser = getCurrentUser();
        document.setCreator(currentUser);
        document.setStatus(DocumentStatus.OFFEN);
        return ResponseEntity.status(HttpStatus.CREATED).body(documentRepository.save(document));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        User currentUser = getCurrentUser();
        // RBAC Check: Only creator, assigned reviewer or admin
        if (currentUser.getRole() != Role.ADMIN && 
            !document.getCreator().getId().equals(currentUser.getId()) && 
            (document.getReviewer() == null || !document.getReviewer().getId().equals(currentUser.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return ResponseEntity.ok(document);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Document> updateDocument(@PathVariable UUID id, @RequestBody Document documentDetails) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        User currentUser = getCurrentUser();
        
        // Only creator or admin can update the text, reviewer can edit if assigned
        if (currentUser.getRole() != Role.ADMIN && 
            !document.getCreator().getId().equals(currentUser.getId()) && 
            (document.getReviewer() == null || !document.getReviewer().getId().equals(currentUser.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // If status is translated or confirmed, user cannot edit anymore (unless admin or reviewer)
        if (currentUser.getRole() == Role.USER && document.getStatus() != DocumentStatus.OFFEN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Document is in review and cannot be edited by user");
        }

        document.setOriginalText(documentDetails.getOriginalText());
        document.setTranslatedText(documentDetails.getTranslatedText());
        document.setSourceLanguage(documentDetails.getSourceLanguage());
        document.setTargetLanguage(documentDetails.getTargetLanguage());
        document.setAutoTranslated(documentDetails.isAutoTranslated());

        return ResponseEntity.ok(documentRepository.save(document));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Document> assignReviewer(@PathVariable UUID id, @RequestParam UUID reviewerId) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        User currentUser = getCurrentUser();
        if (!document.getCreator().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only creator can assign reviewer");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reviewer not found"));
        
        if (reviewer.getRole() != Role.REVIEWER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not a reviewer");
        }

        document.setReviewer(reviewer);
        document.setStatus(DocumentStatus.UEBERSETZT); // Status changes to translated when submitted for review
        
        return ResponseEntity.ok(documentRepository.save(document));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<Document> updateStatus(@PathVariable UUID id, @RequestParam DocumentStatus status) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        User currentUser = getCurrentUser();
        
        // Reviewer can set to BESTAETIGT
        if (currentUser.getRole() == Role.REVIEWER) {
            if (document.getReviewer() == null || !document.getReviewer().getId().equals(currentUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this reviewer");
            }
            document.setStatus(status);
        } else if (currentUser.getRole() == Role.ADMIN) {
            document.setStatus(status);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return ResponseEntity.ok(documentRepository.save(document));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        User currentUser = getCurrentUser();
        
        if (currentUser.getRole() == Role.ADMIN) {
            documentRepository.delete(document);
        } else if (currentUser.getRole() == Role.USER && document.getCreator().getId().equals(currentUser.getId())) {
            documentRepository.delete(document);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/reviewers")
    public ResponseEntity<List<User>> getReviewers() {
        return ResponseEntity.ok(userRepository.findAllByRole(Role.REVIEWER));
    }
}

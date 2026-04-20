package com.translationapp.service;

import com.translationapp.model.Document;
import com.translationapp.model.DocumentStatus;
import com.translationapp.model.Role;
import com.translationapp.model.User;
import com.translationapp.repository.DocumentRepository;
import com.translationapp.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    public Page<Document> getDocumentsForUser(User currentUser, Pageable pageable) {
        if (currentUser.getRole() == Role.ADMIN) {
            return documentRepository.findAll(pageable);
        } else if (currentUser.getRole() == Role.REVIEWER) {
            return documentRepository.findAllByReviewer(currentUser, pageable);
        } else {
            return documentRepository.findAllByCreator(currentUser, pageable);
        }
    }

    public Document createDocument(Document document, User creator) {
        document.setCreator(creator);
        document.setStatus(DocumentStatus.OFFEN);
        if (document.getTitle() == null || document.getTitle().isEmpty()) {
            document.setTitle("Unbenanntes Dokument");
        }
        return documentRepository.save(document);
    }

    public Document getDocumentById(UUID id, User currentUser) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        validateAccess(document, currentUser);
        return document;
    }

    public Document updateDocument(UUID id, Document documentDetails, User currentUser) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        // Only creator or assigned reviewer can update the text. Admin is read-only.
        if (currentUser.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins have read-only access to document content");
        }

        validateAccess(document, currentUser);

        // If status is in review or completed, user cannot edit anymore (unless in correction or admin/reviewer)
        if (currentUser.getRole() == Role.USER && 
            document.getStatus() != DocumentStatus.OFFEN && 
            document.getStatus() != DocumentStatus.KORREKTUR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Document is in review and cannot be edited by user");
        }

        document.setTitle(documentDetails.getTitle());
        document.setOriginalText(documentDetails.getOriginalText());
        document.setTranslatedText(documentDetails.getTranslatedText());
        document.setSourceLanguage(documentDetails.getSourceLanguage());
        document.setTargetLanguage(documentDetails.getTargetLanguage());
        document.setAutoTranslated(documentDetails.isAutoTranslated());

        return documentRepository.save(document);
    }

    public Document assignReviewer(UUID documentId, UUID reviewerId, LocalDateTime deadline, User currentUser) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (currentUser.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admins cannot assign reviewers");
        }
        if (!document.getCreator().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only creator can assign reviewer");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reviewer not found"));
        
        if (reviewer.getRole() != Role.REVIEWER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not a reviewer");
        }

        document.setReviewer(reviewer);
        document.setReviewDeadline(deadline);
        document.setStatus(DocumentStatus.IN_PRUEFUNG);
        
        return documentRepository.save(document);
    }

    public Document updateStatus(UUID id, DocumentStatus status, User currentUser) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (currentUser.getRole() == Role.REVIEWER) {
            if (document.getReviewer() == null || !document.getReviewer().getId().equals(currentUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this reviewer");
            }
            document.setStatus(status);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return documentRepository.save(document);
    }

    public void deleteDocument(UUID id, User currentUser) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (currentUser.getRole() == Role.ADMIN) {
            documentRepository.delete(document);
        } else if (currentUser.getRole() == Role.USER && document.getCreator().getId().equals(currentUser.getId())) {
            documentRepository.delete(document);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    public List<User> getReviewers() {
        return userRepository.findAllByRole(Role.REVIEWER);
    }

    private void validateAccess(Document document, User currentUser) {
        if (currentUser.getRole() != Role.ADMIN && 
            !document.getCreator().getId().equals(currentUser.getId()) && 
            (document.getReviewer() == null || !document.getReviewer().getId().equals(currentUser.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }
}

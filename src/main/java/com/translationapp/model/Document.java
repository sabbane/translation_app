package com.translationapp.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String originalText;

    @Column(columnDefinition = "TEXT")
    private String translatedText;

    private String sourceLanguage;
    private String targetLanguage;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status = DocumentStatus.OFFEN;

    private boolean isAutoTranslated = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    public Document() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }

    public String getTranslatedText() { return translatedText; }
    public void setTranslatedText(String translatedText) { this.translatedText = translatedText; }

    public String getSourceLanguage() { return sourceLanguage; }
    public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }

    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }

    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }

    public boolean isAutoTranslated() { return isAutoTranslated; }
    public void setAutoTranslated(boolean autoTranslated) { isAutoTranslated = autoTranslated; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public User getReviewer() { return reviewer; }
    public void setReviewer(User reviewer) { this.reviewer = reviewer; }

    // Manueller Builder
    public static DocumentBuilder builder() {
        return new DocumentBuilder();
    }

    public static class DocumentBuilder {
        private String originalText;
        private String translatedText;
        private String sourceLanguage;
        private String targetLanguage;
        private User creator;

        public DocumentBuilder originalText(String originalText) { this.originalText = originalText; return this; }
        public DocumentBuilder translatedText(String translatedText) { this.translatedText = translatedText; return this; }
        public DocumentBuilder sourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; return this; }
        public DocumentBuilder targetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; return this; }
        public DocumentBuilder creator(User creator) { this.creator = creator; return this; }

        public Document build() {
            Document doc = new Document();
            doc.setOriginalText(originalText);
            doc.setTranslatedText(translatedText);
            doc.setSourceLanguage(sourceLanguage);
            doc.setTargetLanguage(targetLanguage);
            doc.setCreator(creator);
            return doc;
        }
    }
}

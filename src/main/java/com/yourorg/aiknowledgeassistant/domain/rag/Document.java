package com.yourorg.aiknowledgeassistant.domain.rag;

import com.yourorg.aiknowledgeassistant.domain.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType = DocumentType.SUPPORTING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus processingStatus = DocumentStatus.QUEUED;

    @Column(columnDefinition = "TEXT")
    private String processingError;

    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(columnDefinition = "BYTEA")
    private byte[] sourceContent;

    @Column(columnDefinition = "TEXT")
    private String extractedText;

    @Column(nullable = false)
    private Integer extractedCharacters = 0;

    private Integer pageCount;

    @Column(nullable = false)
    private Integer chunkCount = 0;

    private LocalDateTime processingStartedAt;
    private LocalDateTime processingCompletedAt;

    protected Document() {}

    public Document(String filename, User uploadedBy, DocumentType documentType, byte[] sourceContent) {
        this.filename = filename;
        this.uploadedBy = uploadedBy;
        this.documentType = documentType == null ? DocumentType.SUPPORTING : documentType;
        this.sourceContent = sourceContent;
    }

    public void markParsing() {
        processingStatus = DocumentStatus.PARSING;
        processingError = null;
        processingStartedAt = LocalDateTime.now();
    }

    public void markEmbedding(ParsedExtraction extraction) {
        processingStatus = DocumentStatus.EMBEDDING;
        extractedText = extraction.content();
        extractedCharacters = extraction.content().length();
        pageCount = extraction.pageCount();
    }

    public void markReady(int completedChunks) {
        processingStatus = DocumentStatus.READY;
        chunkCount = completedChunks;
        processingCompletedAt = LocalDateTime.now();
        sourceContent = null;
    }

    public void markFailed(String message) {
        processingStatus = DocumentStatus.FAILED;
        processingError = message;
        processingCompletedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getFilename() { return filename; }
    public User getUploadedBy() { return uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public DocumentType getDocumentType() { return documentType; }
    public DocumentStatus getProcessingStatus() { return processingStatus; }
    public String getProcessingError() { return processingError; }
    public byte[] getSourceContent() { return sourceContent; }
    public String getExtractedText() { return extractedText; }
    public Integer getExtractedCharacters() { return extractedCharacters; }
    public Integer getPageCount() { return pageCount; }
    public Integer getChunkCount() { return chunkCount; }

    public record ParsedExtraction(String content, Integer pageCount) {}
}

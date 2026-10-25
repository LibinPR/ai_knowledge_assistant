package com.yourorg.aiknowledgeassistant.domain.career;

import com.yourorg.aiknowledgeassistant.domain.rag.Document;
import com.yourorg.aiknowledgeassistant.domain.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "career_matches")
public class CareerMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_document_id", nullable = false)
    private Document resumeDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_description_document_id", nullable = false)
    private Document jobDescriptionDocument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CareerMatchStatus status;

    @Column(columnDefinition = "TEXT")
    private String reportJson;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected CareerMatch() {}

    public CareerMatch(User owner, Document resumeDocument, Document jobDescriptionDocument) {
        this.owner = owner;
        this.resumeDocument = resumeDocument;
        this.jobDescriptionDocument = jobDescriptionDocument;
        this.status = CareerMatchStatus.GENERATING;
    }

    public void markReady(String reportJson) {
        this.status = CareerMatchStatus.READY;
        this.reportJson = reportJson;
        this.errorMessage = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = CareerMatchStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public Document getResumeDocument() { return resumeDocument; }
    public Document getJobDescriptionDocument() { return jobDescriptionDocument; }
    public CareerMatchStatus getStatus() { return status; }
    public String getReportJson() { return reportJson; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

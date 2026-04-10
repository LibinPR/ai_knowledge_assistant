package com.yourorg.aiknowledgeassistant.domain.rag;

import com.yourorg.aiknowledgeassistant.domain.user.User;
import jakarta.persistence.*;
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

    protected Document() {}

    public Document(String filename, User uploadedBy) {
        this.filename = filename;
        this.uploadedBy = uploadedBy;
    }

    public Long getId() { return id; }
    public String getFilename() { return filename; }
    public User getUploadedBy() { return uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
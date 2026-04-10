package com.yourorg.aiknowledgeassistant.domain.rag;


import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "vector(384)" , insertable = true , updatable = true)
    private Float[] embedding;

    protected DocumentChunk() {}

    public DocumentChunk(Document document, String content, Float[] embedding) {
        this.document = document;
        this.content = content;
        this.embedding = embedding;
    }

    public Long getId() { return id; }
    public Document getDocument() { return document; }
    public String getContent() { return content; }
    public Float[] getEmbedding() { return embedding; }
}

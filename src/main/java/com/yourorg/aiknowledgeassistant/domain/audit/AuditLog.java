package com.yourorg.aiknowledgeassistant.domain.audit;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(nullable = false)
    private String action;

    private String entity;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "metadata")
    private String metadata;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected AuditLog() {}

    public AuditLog(
            Long actorUserId,
            String action,
            String entity,
            Long entityId,
            String metadata
    ) {
        this.actorUserId = actorUserId;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.metadata = metadata;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

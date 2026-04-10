package com.yourorg.aiknowledgeassistant.domain.user;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_tokens_jti", columnList = "jti"),
                @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")
        }
)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    protected RefreshToken() {}

    public RefreshToken(User user, String jti, Instant expiresAt) {
        this.user = user;
        this.jti = jti;
        this.expiresAt = expiresAt;
        this.revoked = false;
        this.createdAt = Instant.now();
    }

    public User getUser() {
        return user;
    }

    public String getJti() {
        return jti;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void revoke() {
        this.revoked = true;
    }
}
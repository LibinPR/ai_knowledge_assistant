package com.yourorg.aiknowledgeassistant.domain.user;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_credentials")
public class UserCredentials {

    @Id
    @Column(name = "user_id" , nullable = false)
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY , optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "password_hash" , nullable = false , length = 255)
    private String passwordHash;

    @Column(name = "password_updated_at" , nullable = false)
    private Instant passwordUpdatedAt;

    protected UserCredentials() {}

    public UserCredentials(User user , String passwordHash) {
        this.user = user;
        this.passwordHash = passwordHash;
        this.passwordUpdatedAt = Instant.now();
    }

    public User getUser() {
        return user;
    }

//used protected getPasswordHash for safety and avoid password leaks
    protected String getPasswordHash() {
        return passwordHash;
    }

    public Instant getPasswordUpdatedAt() {
        return passwordUpdatedAt;
    }
}

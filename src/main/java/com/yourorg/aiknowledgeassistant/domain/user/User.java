package com.yourorg.aiknowledgeassistant.domain.user;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_email" , columnNames = "email")
        }
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false , length = 255)
    private String email;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean locked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "token_version" , nullable = false)
    private int tokenVersion = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    protected User() {}

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public User(String email) {
        this.email = email;
        this.enabled = true;
        this.locked = false;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // getters only for now
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

//    public boolean isEnabled() {
//        return enabled;
//    }
//
//    public boolean isLocked() {
//        return locked;
//    }

    public boolean canAuthenticate() {
        return enabled && !locked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public Set<String> rolesNames() {
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toUnmodifiableSet());
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public void incrementTokenVersion() {
        this.tokenVersion++;
    }
}

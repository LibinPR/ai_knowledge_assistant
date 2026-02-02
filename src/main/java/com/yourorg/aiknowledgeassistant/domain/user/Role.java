package com.yourorg.aiknowledgeassistant.domain.user;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "roles" ,
        uniqueConstraints = {
        @UniqueConstraint(name = "uq_roles_name" , columnNames = "name")
        }
)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false , length = 50)
    private String name;

    @Column(nullable = false , length = 255)
    private String description;

    @Column(name = "created_at" , nullable = false , updatable = false)
    private Instant createdAt;

    protected Role() {}

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

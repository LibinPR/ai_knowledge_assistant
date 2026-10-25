package com.yourorg.aiknowledgeassistant.domain.workspace;

import com.yourorg.aiknowledgeassistant.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "workspace_memberships")
public class WorkspaceMembership {

    @EmbeddedId
    private WorkspaceMembershipId id;

    @MapsId("workspaceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkspaceMembershipRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WorkspaceMembership() {
    }

    public WorkspaceMembership(Workspace workspace, User user, WorkspaceMembershipRole role) {
        this.id = new WorkspaceMembershipId(workspace.getId(), user.getId());
        this.workspace = workspace;
        this.user = user;
        this.role = role;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Workspace getWorkspace() { return workspace; }
    public User getUser() { return user; }
    public WorkspaceMembershipRole getRole() { return role; }
}

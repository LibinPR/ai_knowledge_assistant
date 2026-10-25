package com.yourorg.aiknowledgeassistant.domain.workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class WorkspaceMembershipId implements Serializable {

    @Column(name = "workspace_id")
    private Long workspaceId;

    @Column(name = "user_id")
    private Long userId;

    protected WorkspaceMembershipId() {
    }

    public WorkspaceMembershipId(Long workspaceId, Long userId) {
        this.workspaceId = workspaceId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WorkspaceMembershipId that)) return false;
        return Objects.equals(workspaceId, that.workspaceId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspaceId, userId);
    }
}

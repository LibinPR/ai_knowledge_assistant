package com.yourorg.aiknowledgeassistant.dto;

import com.yourorg.aiknowledgeassistant.domain.workspace.Workspace;
import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembershipRole;

public record WorkspaceResponse(
        Long id,
        String name,
        String slug,
        WorkspaceMembershipRole role
) {
    public static WorkspaceResponse from(
            Workspace workspace,
            WorkspaceMembershipRole role) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                role
        );
    }
}

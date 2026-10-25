package com.yourorg.aiknowledgeassistant.service.workspace;

import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembership;
import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembershipId;
import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembershipRole;
import com.yourorg.aiknowledgeassistant.repository.workspace.WorkspaceMembershipRepository;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAccessService {

    private final WorkspaceMembershipRepository membershipRepository;

    public WorkspaceAccessService(
            WorkspaceMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    /**
     * Requires the user to be a workspace member with the requested
     * permission level or a higher one.
     *
     * OWNER > ADMIN > MEMBER > VIEWER
     *
     * Global application roles do not bypass workspace membership.
     */
    public WorkspaceMembership requireRole(
            Long workspaceId,
            Long userId,
            WorkspaceMembershipRole requiredRole) {

        if (workspaceId == null || userId == null || requiredRole == null) {
            throw new IllegalArgumentException(
                    "Workspace ID, user ID, and required role are required");
        }

        WorkspaceMembershipId membershipId =
                new WorkspaceMembershipId(workspaceId, userId);

        WorkspaceMembership membership = membershipRepository
                .findById(membershipId)
                .orElseThrow(() ->
                        new SecurityException(
                                "You do not have access to this workspace"));

        if (!hasRequiredRole(membership.getRole(), requiredRole)) {
            throw new SecurityException(
                    "Your workspace role does not permit this operation");
        }

        return membership;
    }

    private boolean hasRequiredRole(
            WorkspaceMembershipRole actualRole,
            WorkspaceMembershipRole requiredRole) {

        return switch (requiredRole) {
            case VIEWER -> true;
            case MEMBER -> actualRole != WorkspaceMembershipRole.VIEWER;
            case ADMIN -> actualRole == WorkspaceMembershipRole.ADMIN
                    || actualRole == WorkspaceMembershipRole.OWNER;
            case OWNER -> actualRole == WorkspaceMembershipRole.OWNER;
        };
    }
}

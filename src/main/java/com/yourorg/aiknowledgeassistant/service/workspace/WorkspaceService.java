package com.yourorg.aiknowledgeassistant.service.workspace;

import com.yourorg.aiknowledgeassistant.domain.user.User;
import com.yourorg.aiknowledgeassistant.domain.workspace.Workspace;
import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembership;
import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembershipRole;
import com.yourorg.aiknowledgeassistant.dto.WorkspaceResponse;
import com.yourorg.aiknowledgeassistant.repository.user.UserRepository;
import com.yourorg.aiknowledgeassistant.repository.workspace.WorkspaceMembershipRepository;
import com.yourorg.aiknowledgeassistant.repository.workspace.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class WorkspaceService {

    private static final Pattern NON_SLUG_CHARS =
            Pattern.compile("[^a-z0-9]+");

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository membershipRepository,
            UserRepository userRepository) {
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listWorkspaces(Long userId) {
        return membershipRepository.findByUser_Id(userId)
                .stream()
                .map(membership -> WorkspaceResponse.from(
                        membership.getWorkspace(),
                        membership.getRole()))
                .toList();
    }

    @Transactional
    public WorkspaceResponse createWorkspace(Long userId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Workspace name must not be blank");
        }

        String normalizedName = name.trim();

        if (normalizedName.length() > 255) {
            throw new IllegalArgumentException(
                    "Workspace name must not exceed 255 characters");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        String slug = generateUniqueSlug(normalizedName);

        Workspace workspace = workspaceRepository.save(
                new Workspace(normalizedName, slug, user));

        WorkspaceMembership membership = membershipRepository.save(
                new WorkspaceMembership(
                        workspace,
                        user,
                        WorkspaceMembershipRole.OWNER));

        return WorkspaceResponse.from(
                membership.getWorkspace(),
                membership.getRole());
    }

    private String generateUniqueSlug(String name) {
        String normalized = Normalizer.normalize(
                name,
                Normalizer.Form.NFD);

        String base = NON_SLUG_CHARS
                .matcher(normalized
                        .replaceAll("\\p{M}", "")
                        .toLowerCase(Locale.ROOT))
                .replaceAll("-")
                .replaceAll("^-|-$", "");

        if (base.isBlank()) {
            base = "workspace";
        }

        // Keep room for a numeric suffix within the 100-character DB limit.
        base = base.substring(0, Math.min(base.length(), 80));

        String candidate = base;
        int suffix = 2;

        while (workspaceRepository.existsBySlug(candidate)) {
            String ending = "-" + suffix++;
            candidate = base.substring(
                    0, Math.min(base.length(), 100 - ending.length()))
                    + ending;
        }

        return candidate;
    }
}

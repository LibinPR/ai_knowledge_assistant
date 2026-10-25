package com.yourorg.aiknowledgeassistant.repository.workspace;

import com.yourorg.aiknowledgeassistant.domain.workspace.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    Optional<Workspace> findBySlug(String slug);
    boolean existsBySlug(String slug);
}

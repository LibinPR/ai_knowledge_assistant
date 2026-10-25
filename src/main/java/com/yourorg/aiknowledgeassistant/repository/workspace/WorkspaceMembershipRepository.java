package com.yourorg.aiknowledgeassistant.repository.workspace;

import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembership;
import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembershipId;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, WorkspaceMembershipId> {

  List<WorkspaceMembership> findByUser_Id(Long userId);
}

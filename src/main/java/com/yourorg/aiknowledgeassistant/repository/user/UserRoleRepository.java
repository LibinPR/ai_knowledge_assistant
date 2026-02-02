package com.yourorg.aiknowledgeassistant.repository.user;

import com.yourorg.aiknowledgeassistant.domain.user.Role;
import com.yourorg.aiknowledgeassistant.domain.user.User;
import com.yourorg.aiknowledgeassistant.domain.user.UserRole;
import com.yourorg.aiknowledgeassistant.domain.user.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole , UserRoleId> {
    List<UserRole> findByUser(User user);
    boolean existsByUserAndRole(User user , Role role);
}

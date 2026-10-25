package com.yourorg.aiknowledgeassistant.service.user;

import com.yourorg.aiknowledgeassistant.domain.user.Role;
import com.yourorg.aiknowledgeassistant.domain.user.User;
import com.yourorg.aiknowledgeassistant.domain.workspace.Workspace;
import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembership;
import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembershipRole;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentRepository;
import com.yourorg.aiknowledgeassistant.repository.user.RoleRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserCredentialsRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserRoleRepository;
import com.yourorg.aiknowledgeassistant.repository.workspace.WorkspaceMembershipRepository;
import com.yourorg.aiknowledgeassistant.repository.workspace.WorkspaceRepository;
import com.yourorg.aiknowledgeassistant.service.audit.AuditService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserCredentialsRepository credentialsRepository = mock(UserCredentialsRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuditService auditService = mock(AuditService.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final WorkspaceRepository workspaceRepository = mock(WorkspaceRepository.class);
    private final WorkspaceMembershipRepository membershipRepository = mock(WorkspaceMembershipRepository.class);
    private final UserService service = new UserService(
            userRepository, credentialsRepository, roleRepository, userRoleRepository,
            passwordEncoder, auditService, documentRepository, workspaceRepository, membershipRepository);

    @Test
    void provisionsAnOwnerPersonalWorkspaceForEachRegisteredUser() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 7L);
            return user;
        });
        when(passwordEncoder.encode("secure-password")).thenReturn("hash");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(mock(Role.class)));
        when(workspaceRepository.saveAndFlush(any(Workspace.class))).thenAnswer(invocation -> {
            Workspace workspace = invocation.getArgument(0);
            ReflectionTestUtils.setField(workspace, "id", 11L);
            return workspace;
        });

        Long userId = service.registerUser("ada@example.com", "secure-password");

        ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
        ArgumentCaptor<WorkspaceMembership> membershipCaptor = ArgumentCaptor.forClass(WorkspaceMembership.class);
        org.mockito.Mockito.verify(workspaceRepository).saveAndFlush(workspaceCaptor.capture());
        org.mockito.Mockito.verify(membershipRepository).save(membershipCaptor.capture());

        Workspace workspace = workspaceCaptor.getValue();
        WorkspaceMembership membership = membershipCaptor.getValue();
        assertThat(userId).isEqualTo(7L);
        assertThat(workspace.getName()).isEqualTo("Personal");
        assertThat(workspace.getSlug()).isEqualTo("personal-7");
        assertThat(workspace.getCreatedBy().getId()).isEqualTo(7L);
        assertThat(membership.getWorkspace()).isSameAs(workspace);
        assertThat(membership.getUser().getId()).isEqualTo(7L);
        assertThat(membership.getRole()).isEqualTo(WorkspaceMembershipRole.OWNER);
    }
}

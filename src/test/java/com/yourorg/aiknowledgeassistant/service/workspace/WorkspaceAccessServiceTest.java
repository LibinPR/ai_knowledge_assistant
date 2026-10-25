package com.yourorg.aiknowledgeassistant.service.workspace;

import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembership;
import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembershipId;
import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembershipRole;
import com.yourorg.aiknowledgeassistant.repository.workspace.WorkspaceMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceAccessServiceTest {

    private static final Long WORKSPACE_ID = 10L;
    private static final Long USER_ID = 20L;

    @Mock
    private WorkspaceMembershipRepository membershipRepository;

    private WorkspaceAccessService accessService;

    @BeforeEach
    void setUp() {
        accessService = new WorkspaceAccessService(membershipRepository);
    }

    @ParameterizedTest
    @EnumSource(WorkspaceMembershipRole.class)
    void everyMemberRoleCanRead(WorkspaceMembershipRole actualRole) {
        stubMembership(actualRole);

        WorkspaceMembership result = accessService.requireRole(
                WORKSPACE_ID,
                USER_ID,
                WorkspaceMembershipRole.VIEWER);

        assertEquals(actualRole, result.getRole());
    }

    @ParameterizedTest
    @MethodSource("writePermissions")
    void onlyMemberAndAboveCanPerformMemberOperations(
            WorkspaceMembershipRole actualRole,
            boolean allowed) {
        stubMembership(actualRole);

        if (allowed) {
            assertDoesNotThrow(() -> accessService.requireRole(
                    WORKSPACE_ID,
                    USER_ID,
                    WorkspaceMembershipRole.MEMBER));
        } else {
            assertThrows(SecurityException.class, () ->
                    accessService.requireRole(
                            WORKSPACE_ID,
                            USER_ID,
                            WorkspaceMembershipRole.MEMBER));
        }
    }

    static Stream<Arguments> writePermissions() {
        return Stream.of(
                Arguments.of(WorkspaceMembershipRole.OWNER, true),
                Arguments.of(WorkspaceMembershipRole.ADMIN, true),
                Arguments.of(WorkspaceMembershipRole.MEMBER, true),
                Arguments.of(WorkspaceMembershipRole.VIEWER, false)
        );
    }

    @ParameterizedTest
    @MethodSource("adminPermissions")
    void onlyAdminAndOwnerCanPerformAdminOperations(
            WorkspaceMembershipRole actualRole,
            boolean allowed) {
        stubMembership(actualRole);

        if (allowed) {
            assertDoesNotThrow(() -> accessService.requireRole(
                    WORKSPACE_ID,
                    USER_ID,
                    WorkspaceMembershipRole.ADMIN));
        } else {
            assertThrows(SecurityException.class, () ->
                    accessService.requireRole(
                            WORKSPACE_ID,
                            USER_ID,
                            WorkspaceMembershipRole.ADMIN));
        }
    }

    static Stream<Arguments> adminPermissions() {
        return Stream.of(
                Arguments.of(WorkspaceMembershipRole.OWNER, true),
                Arguments.of(WorkspaceMembershipRole.ADMIN, true),
                Arguments.of(WorkspaceMembershipRole.MEMBER, false),
                Arguments.of(WorkspaceMembershipRole.VIEWER, false)
        );
    }

    @ParameterizedTest
    @EnumSource(value = WorkspaceMembershipRole.class,
            names = {"OWNER", "ADMIN", "MEMBER", "VIEWER"})
    void onlyOwnerCanPerformOwnerOperations(
            WorkspaceMembershipRole actualRole) {
        stubMembership(actualRole);

        if (actualRole == WorkspaceMembershipRole.OWNER) {
            assertDoesNotThrow(() -> accessService.requireRole(
                    WORKSPACE_ID,
                    USER_ID,
                    WorkspaceMembershipRole.OWNER));
        } else {
            assertThrows(SecurityException.class, () ->
                    accessService.requireRole(
                            WORKSPACE_ID,
                            USER_ID,
                            WorkspaceMembershipRole.OWNER));
        }
    }

    @Test
    void rejectsUserWithoutWorkspaceMembership() {
        when(membershipRepository.findById(
                new WorkspaceMembershipId(WORKSPACE_ID, USER_ID)))
                .thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () ->
                accessService.requireRole(
                        WORKSPACE_ID,
                        USER_ID,
                        WorkspaceMembershipRole.VIEWER));
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () ->
                accessService.requireRole(
                        null,
                        USER_ID,
                        WorkspaceMembershipRole.VIEWER));

        assertThrows(IllegalArgumentException.class, () ->
                accessService.requireRole(
                        WORKSPACE_ID,
                        null,
                        WorkspaceMembershipRole.VIEWER));

        assertThrows(IllegalArgumentException.class, () ->
                accessService.requireRole(
                        WORKSPACE_ID,
                        USER_ID,
                        null));

        verifyNoInteractions(membershipRepository);
    }

    private void stubMembership(WorkspaceMembershipRole role) {
        WorkspaceMembership membership = mock(WorkspaceMembership.class);
        when(membership.getRole()).thenReturn(role);

        when(membershipRepository.findById(
                new WorkspaceMembershipId(WORKSPACE_ID, USER_ID)))
                .thenReturn(Optional.of(membership));
    }
}

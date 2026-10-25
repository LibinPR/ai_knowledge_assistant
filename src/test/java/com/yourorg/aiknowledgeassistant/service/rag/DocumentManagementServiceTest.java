package com.yourorg.aiknowledgeassistant.service.rag;

import com.yourorg.aiknowledgeassistant.domain.rag.Document;
import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembershipRole;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentRepository;
import com.yourorg.aiknowledgeassistant.service.workspace.WorkspaceAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentManagementServiceTest {

    private static final Long WORKSPACE_ID = 10L;
    private static final Long USER_ID = 20L;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private WorkspaceAccessService workspaceAccessService;

    private DocumentManagementService service;

    @BeforeEach
    void setUp() {
        service = new DocumentManagementService(
                documentRepository,
                workspaceAccessService);
    }

    @Test
    void listsDocumentsAfterCheckingViewerAccess() {
        List<Document> documents = List.of(
                mock(Document.class),
                mock(Document.class));

        when(documentRepository
                .findByWorkspace_IdOrderByUploadedAtDesc(WORKSPACE_ID))
                .thenReturn(documents);

        List<Document> result = service.listWorkspaceDocuments(
                WORKSPACE_ID,
                USER_ID);

        assertEquals(documents, result);

        verify(workspaceAccessService).requireRole(
                WORKSPACE_ID,
                USER_ID,
                WorkspaceMembershipRole.VIEWER);

        verify(documentRepository)
                .findByWorkspace_IdOrderByUploadedAtDesc(WORKSPACE_ID);
    }

    @Test
    void doesNotQueryDocumentsWhenAccessIsDenied() {
        doThrow(new SecurityException("Access denied"))
                .when(workspaceAccessService)
                .requireRole(
                        WORKSPACE_ID,
                        USER_ID,
                        WorkspaceMembershipRole.VIEWER);

        assertThrows(
                SecurityException.class,
                () -> service.listWorkspaceDocuments(
                        WORKSPACE_ID,
                        USER_ID));

        verifyNoInteractions(documentRepository);
    }

    @Test
    void returnsEmptyListWhenWorkspaceHasNoDocuments() {
        when(documentRepository
                .findByWorkspace_IdOrderByUploadedAtDesc(WORKSPACE_ID))
                .thenReturn(List.of());

        List<Document> result = service.listWorkspaceDocuments(
                WORKSPACE_ID,
                USER_ID);

        assertTrue(result.isEmpty());

        verify(workspaceAccessService).requireRole(
                WORKSPACE_ID,
                USER_ID,
                WorkspaceMembershipRole.VIEWER);
    }
}

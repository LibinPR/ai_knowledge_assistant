package com.yourorg.aiknowledgeassistant.service.rag;

import com.yourorg.aiknowledgeassistant.domain.rag.DocumentType;
import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembershipRole;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentChunkRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserRepository;
import com.yourorg.aiknowledgeassistant.repository.workspace.WorkspaceRepository;
import com.yourorg.aiknowledgeassistant.service.workspace.WorkspaceAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.multipart.MultipartFile;
import com.yourorg.aiknowledgeassistant.domain.rag.Document;
import com.yourorg.aiknowledgeassistant.domain.user.User;
import com.yourorg.aiknowledgeassistant.domain.workspace.Workspace;
import org.springframework.transaction.TransactionStatus;

import java.io.IOException;
import static org.mockito.ArgumentMatchers.argThat;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
class WorkspaceDocumentUploadTest {

    private static final Long WORKSPACE_ID = 10L;
    private static final Long USER_ID = 20L;

    @Mock private EmbeddingClient embeddingClient;
    @Mock private DocumentChunkRepository documentChunkRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private TextChunker textChunker;
    @Mock private FileParser fileParser;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private WorkspaceAccessService workspaceAccessService;
    @Mock private MultipartFile file;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                embeddingClient,
                documentChunkRepository,
                documentRepository,
                userRepository,
                textChunker,
                fileParser,
                workspaceRepository,
                transactionManager,
                workspaceAccessService);
    }

    @Test
    void viewerIsRejectedBeforeFileValidationOrReading() throws Exception {
        doThrow(new SecurityException("Insufficient workspace role"))
                .when(workspaceAccessService)
                .requireRole(
                        WORKSPACE_ID,
                        USER_ID,
                        WorkspaceMembershipRole.MEMBER);

        assertThrows(
                SecurityException.class,
                () -> documentService.submitToWorkspace(
                        file,
                        USER_ID,
                        WORKSPACE_ID,
                        DocumentType.SUPPORTING));

        verify(workspaceAccessService).requireRole(
                WORKSPACE_ID,
                USER_ID,
                WorkspaceMembershipRole.MEMBER);

        verifyNoInteractions(fileParser, file);
        verifyNoInteractions(userRepository, workspaceRepository, documentRepository);
    }

    @Test
    void authorizationIsCheckedBeforeFileValidation() throws Exception {
        doThrow(new SecurityException("No workspace membership"))
                .when(workspaceAccessService)
                .requireRole(
                        WORKSPACE_ID,
                        USER_ID,
                        WorkspaceMembershipRole.MEMBER);

        assertThrows(
                SecurityException.class,
                () -> documentService.submitToWorkspace(
                        file,
                        USER_ID,
                        WORKSPACE_ID,
                        DocumentType.SUPPORTING));

        verifyNoInteractions(fileParser);
        verifyNoInteractions(documentRepository);
    }

    
    @Test
     void memberCanUploadDocumentToWorkspace() throws Exception {
        User user = mock(User.class);
        Workspace workspace = mock(Workspace.class);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);

        when(file.getOriginalFilename()).thenReturn("sample.pdf");
        when(file.getBytes()).thenReturn(new byte[] {1, 2, 3});

        when(userRepository.findById(USER_ID))
                .thenReturn(java.util.Optional.of(user));

        when(workspaceRepository.findById(WORKSPACE_ID))
                .thenReturn(java.util.Optional.of(workspace));

        when(transactionManager.getTransaction(any()))
                .thenReturn(transactionStatus);

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Document result = documentService.submitToWorkspace(
                file,
                USER_ID,
                WORKSPACE_ID,
                DocumentType.SUPPORTING);

        assertNotNull(result);

        verify(workspaceAccessService).requireRole(
                WORKSPACE_ID,
                USER_ID,
                WorkspaceMembershipRole.MEMBER);
        verify(fileParser).validate(file);
        verify(documentRepository).save(argThat(document ->
                document.getWorkspace() == workspace
                && document.getUploadedBy() == user
                && document.getDocumentType() == DocumentType.SUPPORTING
                && java.util.Arrays.equals(
                        document.getSourceContent(),
                        new byte[] {1, 2, 3}
                )
        ));
        verify(transactionManager).commit(transactionStatus);
    }

}

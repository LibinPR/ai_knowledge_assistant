package com.yourorg.aiknowledgeassistant.controller;

import com.yourorg.aiknowledgeassistant.auth.dto.DocumentResponse;
import com.yourorg.aiknowledgeassistant.service.rag.DocumentManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.yourorg.aiknowledgeassistant.auth.dto.DocumentResponse;
import com.yourorg.aiknowledgeassistant.domain.rag.Document;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentType;
import com.yourorg.aiknowledgeassistant.service.rag.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/documents")
public class WorkspaceDocumentController {

    private final DocumentManagementService documentManagementService;

    private final DocumentService documentService;

    public WorkspaceDocumentController(
            DocumentManagementService documentManagementService,
            DocumentService documentService) {
        this.documentManagementService = documentManagementService;
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> listDocuments(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal Long userId) {

        List<DocumentResponse> documents = documentManagementService
                .listWorkspaceDocuments(workspaceId, userId)
                .stream()
                .map(DocumentResponse::from)
                .toList();

        return ResponseEntity.ok(documents);
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> uploadDocument(
            @PathVariable Long workspaceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "SUPPORTING") DocumentType documentType,
            @AuthenticationPrincipal Long userId) throws IOException {

        Document document = documentService.submitToWorkspace(
                file,
                userId,
                workspaceId,
                documentType);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(DocumentResponse.from(document));
    }
}

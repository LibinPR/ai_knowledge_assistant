package com.yourorg.aiknowledgeassistant.controller;

import com.yourorg.aiknowledgeassistant.auth.dto.DocumentResponse;
import com.yourorg.aiknowledgeassistant.service.rag.DocumentManagementService;
import com.yourorg.aiknowledgeassistant.service.rag.DocumentService;
import com.yourorg.aiknowledgeassistant.service.rag.RagQueryService;
import com.yourorg.aiknowledgeassistant.domain.rag.Document;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentType;
import com.yourorg.aiknowledgeassistant.dto.DocumentPreviewResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.yourorg.aiknowledgeassistant.dto.QueryResponse;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final DocumentService documentService;
    private final RagQueryService ragQueryService;
    private final DocumentManagementService documentManagementService;

    public RagController(DocumentService documentService,
            RagQueryService ragQueryService,
            DocumentManagementService documentManagementService) {
        this.documentService = documentService;
        this.ragQueryService = ragQueryService;
        this.documentManagementService = documentManagementService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<DocumentResponse> ingest(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "SUPPORTING") DocumentType documentType,
            @AuthenticationPrincipal Long userId) throws IOException {
        Document document = documentService.submit(file, userId, documentType);
        return ResponseEntity.accepted().body(DocumentResponse.from(document));
    }

    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(
            @RequestParam String question,
            @AuthenticationPrincipal Long userId) {

        QueryResponse response = ragQueryService.query(
                                            question,
                                            userId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentResponse>> listDocuments(
            @AuthenticationPrincipal Long userId,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        List<DocumentResponse> documents = documentManagementService
                .listDocuments(userId, isAdmin)
                .stream()
                .map(DocumentResponse::from)
                .toList();

        return ResponseEntity.ok(documents);
    }

    @GetMapping("/documents/{id}/preview")
    public ResponseEntity<DocumentPreviewResponse> previewDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        return ResponseEntity.ok(DocumentPreviewResponse.from(
                documentManagementService.getDocument(id, userId, isAdmin)));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        documentManagementService.deleteDocument(id, userId, isAdmin);

        return ResponseEntity.noContent().build();
    }
}

package com.yourorg.aiknowledgeassistant.controller;

import com.yourorg.aiknowledgeassistant.auth.dto.DocumentResponse;
import com.yourorg.aiknowledgeassistant.service.rag.DocumentManagementService;
import com.yourorg.aiknowledgeassistant.service.rag.DocumentService;
import com.yourorg.aiknowledgeassistant.service.rag.FileParser;
import com.yourorg.aiknowledgeassistant.service.rag.RagQueryService;
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
    private final FileParser fileParser;
    private final RagQueryService ragQueryService;
    private final DocumentManagementService documentManagementService;

    public RagController(DocumentService documentService,
            FileParser fileParser,
            RagQueryService ragQueryService,
            DocumentManagementService documentManagementService) {
        this.documentService = documentService;
        this.fileParser = fileParser;
        this.ragQueryService = ragQueryService;
        this.documentManagementService = documentManagementService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<String> ingest(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Long userId) throws IOException {

        String content = fileParser.parse(file);

        documentService.ingest(file.getOriginalFilename(), content, userId);

        return ResponseEntity.ok("Ingested Successfully");
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
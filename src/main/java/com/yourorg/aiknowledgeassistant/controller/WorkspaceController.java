package com.yourorg.aiknowledgeassistant.controller;

import com.yourorg.aiknowledgeassistant.dto.WorkspaceResponse;
import com.yourorg.aiknowledgeassistant.service.workspace.WorkspaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.yourorg.aiknowledgeassistant.dto.CreateWorkspaceRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> listWorkspaces(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(
                workspaceService.listWorkspaces(userId));
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateWorkspaceRequest request) {

        WorkspaceResponse workspace = workspaceService.createWorkspace(
                userId,
                request.name());

        return ResponseEntity.status(HttpStatus.CREATED).body(workspace);
    }
}

package com.yourorg.aiknowledgeassistant.controller;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yourorg.aiknowledgeassistant.dto.DocumentSummaryResponse;
import com.yourorg.aiknowledgeassistant.service.rag.DocumentManagementService;
import com.yourorg.aiknowledgeassistant.service.user.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String userOnly(){
        return "User profile";
    }

    @GetMapping("/whoami")
    public Object whoami(Authentication authentication){

        return Map.of(
                "principal",authentication.getPrincipal(),
                "authorities",authentication.getAuthorities()
        );
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentSummaryResponse>> listDocuments(
        @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(
            userService.listDocuments(userId)
        );
    }
}

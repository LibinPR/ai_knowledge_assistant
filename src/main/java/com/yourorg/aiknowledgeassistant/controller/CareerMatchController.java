package com.yourorg.aiknowledgeassistant.controller;

import com.yourorg.aiknowledgeassistant.dto.CareerMatchResponse;
import com.yourorg.aiknowledgeassistant.dto.CreateCareerMatchRequest;
import com.yourorg.aiknowledgeassistant.service.career.CareerMatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/career")
public class CareerMatchController {
    private final CareerMatchService service;

    public CareerMatchController(CareerMatchService service) { this.service = service; }

    @PostMapping("/matches")
    public ResponseEntity<CareerMatchResponse> create(@Valid @RequestBody CreateCareerMatchRequest request,
                                                       @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(service.create(request, userId));
    }

    @GetMapping("/matches/{id}")
    public CareerMatchResponse get(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return service.get(id, userId);
    }

    @GetMapping("/matches")
    public List<CareerMatchResponse> list(@AuthenticationPrincipal Long userId) {
        return service.list(userId);
    }
}

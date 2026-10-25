package com.yourorg.aiknowledgeassistant.dto;

import jakarta.validation.constraints.NotNull;

public record CreateCareerMatchRequest(
        @NotNull Long resumeDocumentId,
        @NotNull Long jobDescriptionDocumentId) {
}

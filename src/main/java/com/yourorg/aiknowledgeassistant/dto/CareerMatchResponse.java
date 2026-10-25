package com.yourorg.aiknowledgeassistant.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.aiknowledgeassistant.domain.career.CareerMatch;

public record CareerMatchResponse(
        Long id,
        String status,
        String resumeFilename,
        String jobDescriptionFilename,
        JsonNode report,
        String error,
        String createdAt,
        String updatedAt) {

    public static CareerMatchResponse from(CareerMatch match, ObjectMapper objectMapper) {
        JsonNode report = null;
        if (match.getReportJson() != null) {
            try {
                report = objectMapper.readTree(match.getReportJson());
            } catch (Exception ignored) {
                // A persisted READY report is validated before saving; keep the API resilient if data is edited manually.
            }
        }
        return new CareerMatchResponse(
                match.getId(), match.getStatus().name(),
                match.getResumeDocument().getFilename(),
                match.getJobDescriptionDocument().getFilename(),
                report, match.getErrorMessage(),
                match.getCreatedAt().toString(), match.getUpdatedAt().toString());
    }
}

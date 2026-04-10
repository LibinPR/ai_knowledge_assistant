package com.yourorg.aiknowledgeassistant.auth.dto;

import com.yourorg.aiknowledgeassistant.domain.rag.Document;

import java.time.LocalDateTime;

public record DocumentResponse(
    Long id,
    String filename,
    String uploadedBy,
    LocalDateTime uploadedAt) {
  public static DocumentResponse from(Document document) {
    return new DocumentResponse(document.getId(),
        document.getFilename(),
        document.getUploadedBy().getEmail(),
        document.getUploadedAt());
  }
}

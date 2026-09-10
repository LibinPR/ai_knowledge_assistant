package com.yourorg.aiknowledgeassistant.auth.dto;

import com.yourorg.aiknowledgeassistant.domain.rag.Document;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentStatus;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentType;

import java.time.LocalDateTime;

public record DocumentResponse(
    Long id,
    String filename,
    String uploadedBy,
    LocalDateTime uploadedAt,
    DocumentType documentType,
    DocumentStatus processingStatus,
    Integer extractedCharacters,
    Integer chunkCount,
    String processingError) {
  public static DocumentResponse from(Document document) {
    return new DocumentResponse(document.getId(),
        document.getFilename(),
        document.getUploadedBy().getEmail(),
        document.getUploadedAt(),
        document.getDocumentType(),
        document.getProcessingStatus(),
        document.getExtractedCharacters(),
        document.getChunkCount(),
        document.getProcessingError());
  }
}

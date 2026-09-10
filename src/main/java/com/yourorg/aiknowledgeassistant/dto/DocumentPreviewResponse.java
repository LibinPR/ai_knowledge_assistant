package com.yourorg.aiknowledgeassistant.dto;

import com.yourorg.aiknowledgeassistant.domain.rag.Document;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentStatus;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentType;

public record DocumentPreviewResponse(
        Long id,
        String filename,
        DocumentType type,
        DocumentStatus status,
        Integer pageCount,
        Integer extractedCharacters,
        Integer chunkCount,
        String processingError,
        String extractedTextPreview) {

    public static DocumentPreviewResponse from(Document document) {
        String text = document.getExtractedText();
        String preview = text == null ? null : text.substring(0, Math.min(text.length(), 2_000));
        return new DocumentPreviewResponse(
                document.getId(), document.getFilename(), document.getDocumentType(),
                document.getProcessingStatus(), document.getPageCount(),
                document.getExtractedCharacters(), document.getChunkCount(),
                document.getProcessingError(), preview);
    }
}

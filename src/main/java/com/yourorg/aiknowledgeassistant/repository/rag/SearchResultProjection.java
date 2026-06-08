package com.yourorg.aiknowledgeassistant.repository.rag;

public interface SearchResultProjection {
    Long getDocumentId();
    String getDocumentName();
    String getContent();
    Double getSimilarity();
}
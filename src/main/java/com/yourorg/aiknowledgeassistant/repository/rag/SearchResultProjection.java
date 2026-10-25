package com.yourorg.aiknowledgeassistant.repository.rag;

public interface SearchResultProjection {
    Long getChunkId();
    Long getDocumentId();
    String getDocumentName();
    String getContent();
    Integer getPageNumber();
    String getSectionTitle();
    Double getSimilarity();
    Double getKeywordScore();
}

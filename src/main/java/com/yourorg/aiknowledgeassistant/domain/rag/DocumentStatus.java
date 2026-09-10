package com.yourorg.aiknowledgeassistant.domain.rag;

public enum DocumentStatus {
    QUEUED,
    PARSING,
    EMBEDDING,
    READY,
    FAILED
}

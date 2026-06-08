package com.yourorg.aiknowledgeassistant.dto;

public record SearchResult(
  Long documentId,
  String documentName,
  String content,
  Double similarity
) {}
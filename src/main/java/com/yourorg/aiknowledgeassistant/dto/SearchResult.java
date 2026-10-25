package com.yourorg.aiknowledgeassistant.dto;

public record SearchResult(
  Long chunkId,
  Long documentId,
  String documentName,
  String content,
  Integer pageNumber,
  String sectionTitle,
  Double similarity,
  Double keywordScore,
  Double hybridScore
) {}

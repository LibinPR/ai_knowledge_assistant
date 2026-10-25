package com.yourorg.aiknowledgeassistant.dto;

public record SourceDto(
  Long documentId,
  String documentName,
  Double similarity,
  Integer pageNumber,
  String sectionTitle,
  String excerpt
) {}

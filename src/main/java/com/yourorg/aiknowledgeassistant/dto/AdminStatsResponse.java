package com.yourorg.aiknowledgeassistant.dto;

public record AdminStatsResponse(
  long users,
  long documents,
  long chunks
) {}
package com.yourorg.aiknowledgeassistant.dto;

import java.util.List;
import com.yourorg.aiknowledgeassistant.dto.SourceDto;

public record QueryResponse(
  String answer,
  List<SourceDto> sources
) {}
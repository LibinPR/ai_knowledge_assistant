package com.yourorg.aiknowledgeassistant.service.rag;

import java.util.List;

public record EmbeddingResponse(List<List<Double>> embeddings) {
}

package com.yourorg.aiknowledgeassistant.service.rag;

import com.yourorg.aiknowledgeassistant.repository.rag.DocumentChunkRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentChunkSummary;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SimilaritySearchService {

    private static final int TOP_K = 5;

    private final EmbeddingClient embeddingClient;
    private final DocumentChunkRepository documentChunkRepository;

    public SimilaritySearchService(EmbeddingClient embeddingClient,
            DocumentChunkRepository documentChunkRepository) {
        this.embeddingClient = embeddingClient;
        this.documentChunkRepository = documentChunkRepository;
    }

    public List<String> search(String question) {
        Float[] embedding = embeddingClient.embed(question);
        String pgvectorFormat = toVectorString(embedding);

        return documentChunkRepository.findSimilar(pgvectorFormat, TOP_K)
                .stream()
                .map(DocumentChunkSummary::getContent)
                .collect(Collectors.toList());
    }

    private String toVectorString(Float[] embedding) {
        return Arrays.stream(embedding)
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}

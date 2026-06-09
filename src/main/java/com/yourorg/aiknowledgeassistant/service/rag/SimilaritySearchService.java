package com.yourorg.aiknowledgeassistant.service.rag;

import com.yourorg.aiknowledgeassistant.repository.rag.DocumentChunkRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.SearchResultProjection;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.yourorg.aiknowledgeassistant.dto.SearchResult;

@Service
public class SimilaritySearchService {

    private static final int TOP_K = 3;
    private static final double MIN_SIMILARITY = 0.60;

    private final EmbeddingClient embeddingClient;
    private final DocumentChunkRepository documentChunkRepository;

    public SimilaritySearchService(EmbeddingClient embeddingClient,
            DocumentChunkRepository documentChunkRepository) {
        this.embeddingClient = embeddingClient;
        this.documentChunkRepository = documentChunkRepository;
    }

    public List<SearchResult> search(
        String question,
        Long userId
        ) {

        Float[] embedding = embeddingClient.embed(question);
        String pgvectorFormat = toVectorString(embedding);

        List<SearchResultProjection> matches = documentChunkRepository.findSimilar(pgvectorFormat , userId, TOP_K);


        List<SearchResult> results = matches.stream()
                .filter(chunk -> chunk.getSimilarity() > MIN_SIMILARITY)
                .sorted((a,b) -> 
                    Double.compare(b.getSimilarity(), a.getSimilarity())
                ) // Sort by similarity
                .map(chunk -> new SearchResult(
                        chunk.getDocumentId(),
                        chunk.getDocumentName(),
                        chunk.getContent(),
                        chunk.getSimilarity()
                ))
                .toList();

        return results;
    }

    private String toVectorString(Float[] embedding) {
        return Arrays.stream(embedding)
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}

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
    private static final double MIN_SIMILARITY = 0.50;

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

        System.out.println("\n========== SIMILARITY SEARCH ==========");
        System.out.println("Question: " + question);
        System.out.println("User ID: " + userId);

        Float[] embedding = embeddingClient.embed(question);

        System.out.println("Embedding Dimensions: " + embedding.length);
        System.out.println("Embedding Sample: " +
            Arrays.toString(Arrays.copyOf(embedding, Math.min(5, embedding.length)))
    );

        String pgvectorFormat = toVectorString(embedding);

        List<SearchResultProjection> matches = documentChunkRepository.findSimilar(pgvectorFormat , userId, TOP_K);

        System.out.println("MIN_SIMILARITY = " + MIN_SIMILARITY);
        System.out.println("Raw Matches Returned = " + matches.size());

        matches.forEach(chunk ->
            System.out.printf(
                    "RAW -> Doc=%s | Similarity=%.4f%n",
                    chunk.getDocumentName(),
                    chunk.getSimilarity()
            )
        );

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

    System.out.println("After Filter Count = " + results.size());

    results.forEach(r ->
            System.out.printf(
                    "FINAL -> Doc=%s | Similarity=%.4f%n",
                    r.documentName(),
                    r.similarity()
            )
    );
    System.out.println("==============================================\n");

        return results;
    }

    private String toVectorString(Float[] embedding) {
        return Arrays.stream(embedding)
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}

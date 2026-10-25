package com.yourorg.aiknowledgeassistant.service.rag;

import com.yourorg.aiknowledgeassistant.dto.SearchResult;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentChunkRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.SearchResultProjection;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SimilaritySearchService {
    private static final int CANDIDATE_K = 12;
    private static final int RESULT_K = 6;
    private static final double MIN_SEMANTIC_SIMILARITY = 0.35;
    private static final int RRF_K = 60;

    private final EmbeddingClient embeddingClient;
    private final DocumentChunkRepository documentChunkRepository;

    public SimilaritySearchService(EmbeddingClient embeddingClient,
                                   DocumentChunkRepository documentChunkRepository) {
        this.embeddingClient = embeddingClient;
        this.documentChunkRepository = documentChunkRepository;
    }

    public List<SearchResult> search(String question, Long userId) {
        Float[] embedding = embeddingClient.embed(question);
        String pgvectorFormat = toVectorString(embedding);

        List<SearchResultProjection> vectorMatches = documentChunkRepository.findSimilar(
                pgvectorFormat, userId, CANDIDATE_K);
        List<SearchResultProjection> keywordMatches = documentChunkRepository.findKeywordMatches(
                question, userId, CANDIDATE_K);

        Map<Long, Candidate> candidates = new LinkedHashMap<>();
        for (int index = 0; index < vectorMatches.size(); index++) {
            SearchResultProjection match = vectorMatches.get(index);
            Candidate candidate = candidates.computeIfAbsent(match.getChunkId(), ignored -> new Candidate(match));
            candidate.vectorRank = index + 1;
            candidate.similarity = safe(match.getSimilarity());
        }
        for (int index = 0; index < keywordMatches.size(); index++) {
            SearchResultProjection match = keywordMatches.get(index);
            Candidate candidate = candidates.computeIfAbsent(match.getChunkId(), ignored -> new Candidate(match));
            candidate.keywordRank = index + 1;
            candidate.keywordScore = safe(match.getKeywordScore());
        }

        return candidates.values().stream()
                .filter(candidate -> candidate.similarity >= MIN_SEMANTIC_SIMILARITY || candidate.keywordScore > 0)
                .sorted(Comparator.comparingDouble(Candidate::hybridScore).reversed())
                .limit(RESULT_K)
                .map(Candidate::toSearchResult)
                .toList();
    }

    
    public List<SearchResult> searchInWorkspace(String question, Long workspaceId) {
        Float[] embedding = embeddingClient.embed(question);
        String pgvectorFormat = toVectorString(embedding);

        List<SearchResultProjection> vectorMatches =
                documentChunkRepository.findSimilarInWorkspace(
                        pgvectorFormat, workspaceId, CANDIDATE_K);

        List<SearchResultProjection> keywordMatches =
                documentChunkRepository.findKeywordMatchesInWorkspace(
                        question, workspaceId, CANDIDATE_K);

        Map<Long, Candidate> candidates = new LinkedHashMap<>();

        for (int index = 0; index < vectorMatches.size(); index++) {
            SearchResultProjection match = vectorMatches.get(index);
            Candidate candidate = candidates.computeIfAbsent(
                    match.getChunkId(), ignored -> new Candidate(match));
            candidate.vectorRank = index + 1;
            candidate.similarity = safe(match.getSimilarity());
        }

        for (int index = 0; index < keywordMatches.size(); index++) {
            SearchResultProjection match = keywordMatches.get(index);
            Candidate candidate = candidates.computeIfAbsent(
                    match.getChunkId(), ignored -> new Candidate(match));
            candidate.keywordRank = index + 1;
            candidate.keywordScore = safe(match.getKeywordScore());
        }

        return candidates.values().stream()
                .filter(candidate ->
                        candidate.similarity >= MIN_SEMANTIC_SIMILARITY
                                || candidate.keywordScore > 0)
                .sorted(Comparator.comparingDouble(Candidate::hybridScore).reversed())
                .limit(RESULT_K)
                .map(Candidate::toSearchResult)
                .toList();
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }

    private String toVectorString(Float[] embedding) {
        return Arrays.stream(embedding)
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static final class Candidate {
        private final SearchResultProjection projection;
        private int vectorRank;
        private int keywordRank;
        private double similarity;
        private double keywordScore;

        private Candidate(SearchResultProjection projection) {
            this.projection = projection;
        }

        private double hybridScore() {
            double vectorContribution = vectorRank == 0 ? 0 : 1.0 / (RRF_K + vectorRank);
            double keywordContribution = keywordRank == 0 ? 0 : 1.0 / (RRF_K + keywordRank);
            return vectorContribution + keywordContribution;
        }

        private SearchResult toSearchResult() {
            return new SearchResult(
                    projection.getChunkId(),
                    projection.getDocumentId(),
                    projection.getDocumentName(),
                    projection.getContent(),
                    projection.getPageNumber(),
                    projection.getSectionTitle() == null ? "General" : projection.getSectionTitle(),
                    similarity,
                    keywordScore,
                    hybridScore());
        }
    }
}

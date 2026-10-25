package com.yourorg.aiknowledgeassistant.service.rag;

import com.yourorg.aiknowledgeassistant.dto.SearchResult;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentChunkRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.SearchResultProjection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimilaritySearchServiceTest {

    private final EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
    private final DocumentChunkRepository chunkRepository = mock(DocumentChunkRepository.class);
    private final SimilaritySearchService service = new SimilaritySearchService(embeddingClient, chunkRepository);

    @Test
    void combinesVectorAndKeywordCandidatesUsingReciprocalRankFusion() {
        when(embeddingClient.embed("deployment guide")).thenReturn(new Float[]{0.1f, 0.2f});
        when(chunkRepository.findSimilar("[0.1,0.2]", 42L, 12)).thenReturn(List.of(
                projection(10L, 100L, "vector-only.md", "Vector evidence", 0.80, 0.0),
                projection(20L, 200L, "both.md", "Combined evidence", 0.70, 0.0),
                projection(30L, 300L, "low-score.md", "Low confidence", 0.20, 0.0)
        ));
        when(chunkRepository.findKeywordMatches("deployment guide", 42L, 12)).thenReturn(List.of(
                projection(20L, 200L, "both.md", "Combined evidence", 0.0, 0.90),
                projection(40L, 400L, "keyword-only.md", "Keyword evidence", 0.0, 0.50)
        ));

        List<SearchResult> results = service.search("deployment guide", 42L);

        assertThat(results).extracting(SearchResult::chunkId)
                .containsExactly(20L, 10L, 40L);
        assertThat(results.getFirst())
                .extracting(SearchResult::similarity, SearchResult::keywordScore)
                .containsExactly(0.70, 0.90);
        assertThat(results.getFirst().hybridScore()).isGreaterThan(results.get(1).hybridScore());
    }

    @Test
    void forwardsTheAuthenticatedOwnerToBothRetrievalQueries() {
        when(embeddingClient.embed("private question")).thenReturn(new Float[]{1.0f});
        when(chunkRepository.findSimilar("[1.0]", 77L, 12)).thenReturn(List.of());
        when(chunkRepository.findKeywordMatches("private question", 77L, 12)).thenReturn(List.of());

        assertThat(service.search("private question", 77L)).isEmpty();
    }

    private SearchResultProjection projection(Long chunkId, Long documentId, String documentName,
                                              String content, Double similarity, Double keywordScore) {
        return new SearchResultProjection() {
            @Override public Long getChunkId() { return chunkId; }
            @Override public Long getDocumentId() { return documentId; }
            @Override public String getDocumentName() { return documentName; }
            @Override public String getContent() { return content; }
            @Override public Integer getPageNumber() { return 1; }
            @Override public String getSectionTitle() { return "Operations"; }
            @Override public Double getSimilarity() { return similarity; }
            @Override public Double getKeywordScore() { return keywordScore; }
        };
    }
}

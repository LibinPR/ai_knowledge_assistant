package com.yourorg.aiknowledgeassistant.service.rag;

import com.yourorg.aiknowledgeassistant.domain.rag.DocumentStatus;
import com.yourorg.aiknowledgeassistant.dto.QueryResponse;
import com.yourorg.aiknowledgeassistant.dto.SearchResult;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yourorg.aiknowledgeassistant.service.workspace.WorkspaceAccessService;

class RagQueryServiceTest {

    private final SimilaritySearchService similaritySearch =
        mock(SimilaritySearchService.class);

        private final GroqClient groqClient =
                mock(GroqClient.class);

        private final DocumentRepository documentRepository =
                mock(DocumentRepository.class);

        private final WorkspaceAccessService workspaceAccessService =
                mock(WorkspaceAccessService.class);

        private final RagQueryService service =
                new RagQueryService(
                        similaritySearch,
                        groqClient,
                        documentRepository,
                        workspaceAccessService);

    @Test
    void tellsTheUserToWaitWhenOwnedDocumentsAreStillProcessing() {
        when(similaritySearch.search("What changed?", 12L)).thenReturn(List.of());
        when(documentRepository.countByUploadedByIdAndProcessingStatusIn(eq(12L), eq(List.of(
                DocumentStatus.QUEUED, DocumentStatus.PARSING, DocumentStatus.EMBEDDING)))).thenReturn(1L);

        QueryResponse response = service.query("What changed?", 12L);

        assertThat(response.answer()).contains("still being processed");
        assertThat(response.sources()).isEmpty();
        verify(groqClient, never()).chat(anyString(), anyString());
    }

    @Test
    void refusesToAnswerWithoutEvidenceWhenNoDocumentsArePending() {
        when(similaritySearch.search("What changed?", 12L)).thenReturn(List.of());
        when(documentRepository.countByUploadedByIdAndProcessingStatusIn(eq(12L), eq(List.of(
                DocumentStatus.QUEUED, DocumentStatus.PARSING, DocumentStatus.EMBEDDING)))).thenReturn(0L);

        QueryResponse response = service.query("What changed?", 12L);

        assertThat(response.answer()).isEqualTo("I don't have enough information to answer that question.");
        assertThat(response.sources()).isEmpty();
        verify(groqClient, never()).chat(anyString(), anyString());
    }

    @Test
    void passesEvidenceToTheModelAndReturnsOrderedDeduplicatedSources() {
        SearchResult lowerRankedSection = result(10L, 1L, "runbook.md", "Overview", 1, "First section", 0.61, 0.0, 0.02);
        SearchResult higherRankedSection = result(11L, 1L, "runbook.md", "Recovery", 3, "Restart the service", 0.75, 0.2, 0.04);
        SearchResult highestRankedDocument = result(20L, 2L, "incident.md", "Impact", 2, "Customer impact", 0.70, 0.4, 0.06);
        when(similaritySearch.search("How do we recover?", 12L))
                .thenReturn(List.of(lowerRankedSection, higherRankedSection, highestRankedDocument));
        when(groqClient.chat(anyString(), eq("How do we recover?"))).thenReturn("Follow the recovery runbook.");

        QueryResponse response = service.query("How do we recover?", 12L);

        assertThat(response.answer()).isEqualTo("Follow the recovery runbook.");
        assertThat(response.sources()).extracting(source -> source.documentId())
                .containsExactly(2L, 1L);
        assertThat(response.sources().get(1))
                .extracting(source -> source.sectionTitle(), source -> source.pageNumber(), source -> source.excerpt())
                .containsExactly("Recovery", 3, "Restart the service");

        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        verify(groqClient).chat(systemPrompt.capture(), eq("How do we recover?"));
        assertThat(systemPrompt.getValue())
                .contains("[Source: runbook.md | section: Overview | page: 1]")
                .contains("[Source: runbook.md | section: Recovery | page: 3]")
                .contains("[Source: incident.md | section: Impact | page: 2]");
    }

    private SearchResult result(Long chunkId, Long documentId, String documentName, String sectionTitle,
                                Integer pageNumber, String content, Double similarity,
                                Double keywordScore, Double hybridScore) {
        return new SearchResult(chunkId, documentId, documentName, content, pageNumber,
                sectionTitle, similarity, keywordScore, hybridScore);
    }
}

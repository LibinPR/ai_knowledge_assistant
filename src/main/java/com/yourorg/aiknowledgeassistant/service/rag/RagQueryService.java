package com.yourorg.aiknowledgeassistant.service.rag;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import com.yourorg.aiknowledgeassistant.dto.SearchResult;
import com.yourorg.aiknowledgeassistant.dto.QueryResponse;
import com.yourorg.aiknowledgeassistant.dto.SourceDto;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentChunkRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentRepository;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentStatus;

@Service
public class RagQueryService {

    private final SimilaritySearchService similaritySearchService;
    private final GroqClient groqClient;
    private final DocumentRepository documentRepository;

    public RagQueryService(SimilaritySearchService similaritySearchService,
                           GroqClient groqClient,
                           DocumentRepository documentRepository) {
        this.similaritySearchService = similaritySearchService;
        this.groqClient = groqClient;
        this.documentRepository = documentRepository;
    }

    public QueryResponse query(String question , Long userId) {
        List<SearchResult> results = similaritySearchService.search(
                                                question,
                                                userId 
                                            );

        if(results.isEmpty()) {
            long pendingDocuments = documentRepository.countByUploadedByIdAndProcessingStatusIn(userId,
                    List.of(DocumentStatus.QUEUED, DocumentStatus.PARSING, DocumentStatus.EMBEDDING));
            if (pendingDocuments > 0) {
                return new QueryResponse("Your document is still being processed. Wait until its status is READY, then try again.", List.of());
            }
            return new QueryResponse("I don't have enough information to answer that question.", List.of());
        }

        String context = results.stream()
                .map(SearchResult::content)
                .collect(Collectors.joining("\n\n---\n\n"));
                

        String systemPrompt = """
                You are a retrieval-augmented AI assistant.

                IMPORTANT:

                If the answer is not explicitly present in the context,
                respond ONLY with:

                "I don't know based on the uploaded documents."

                Do not use your own knowledge.
                Do not explain concepts not present in the context.
                Do not infer or assume missing information.

                Only answer using facts directly found in the context.

                Context:
                """ + context;
       

        String answer = groqClient.chat(systemPrompt,question);

        List<SourceDto> sources = results.stream()
            .sorted((a,b) -> Double.compare(b.similarity(), a.similarity())) // Sort by similarity
                .collect(
                    Collectors.toMap(
                        SearchResult::documentId,
                        r -> new SourceDto(
                            r.documentId(),
                            r.documentName(),
                            r.similarity()
                        ),
                        (existing , replacement) -> existing
                    )
                )
                .values()
                .stream()
                .toList();

        return new QueryResponse(answer , sources);
    }
}

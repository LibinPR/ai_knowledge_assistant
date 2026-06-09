package com.yourorg.aiknowledgeassistant.service.rag;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import com.yourorg.aiknowledgeassistant.dto.SearchResult;
import com.yourorg.aiknowledgeassistant.dto.QueryResponse;
import com.yourorg.aiknowledgeassistant.dto.SourceDto;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentChunkRepository;

@Service
public class RagQueryService {

    private final SimilaritySearchService similaritySearchService;
    private final GroqClient groqClient;

    public RagQueryService(SimilaritySearchService similaritySearchService,
                           GroqClient groqClient) {
        this.similaritySearchService = similaritySearchService;
        this.groqClient = groqClient;
    }

    public QueryResponse query(String question , Long userId) {
        List<SearchResult> results = similaritySearchService.search(
                                                question,
                                                userId 
                                            );

        System.out.println("\n========== RAG DEBUG ==========");
        System.out.println("Question: " + question);
        System.out.println("Results found: " + results.size());

        results.forEach(r ->
            System.out.printf(
                "Doc=%s | Similarity=%.4f%n",
                r.documentName(),
                r.similarity()
            )
        );

        if(results.isEmpty()) {
            return new QueryResponse("I don't have enough information to answer that question.", List.of());
        }

        String context = results.stream()
                .map(SearchResult::content)
                .collect(Collectors.joining("\n\n---\n\n"));
                

        String systemPrompt = """
                You are a retrieval-augmented AI assistant.

                Rules:
                1. Answer ONLY from the provided context.
                2. Do not invent facts and make assumptions.
                3. If information is missing, say:
                    "I don't know based on the uploaded documents."
                4. Keep answers concise and factual.
                5. Quote exact values , dates and amounts whenever available.
                
                Context:
                """ + context;

        System.out.println("\n===== CONTEXT =====");
        System.out.println(context);
        System.out.println("===================\n");

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

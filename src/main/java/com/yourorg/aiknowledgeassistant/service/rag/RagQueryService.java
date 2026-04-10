package com.yourorg.aiknowledgeassistant.service.rag;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagQueryService {

    private final SimilaritySearchService similaritySearchService;
    private final GroqClient groqClient;

    public RagQueryService(SimilaritySearchService similaritySearchService,
                           GroqClient groqClient) {
        this.similaritySearchService = similaritySearchService;
        this.groqClient = groqClient;
    }

    public String query(String question) {
        List<String> relevantChunks = similaritySearchService.search(question);

        if(relevantChunks.isEmpty()) {
            return "I don't have enough information to answer that question.";
        }

        String context = relevantChunks.stream()
                .collect(Collectors.joining("\n\n---\n\n"));

        String systemPrompt = """
                You are a helpful assistant. Answer the user's question using ONLY the context provided below.
                If the answer is not in the context, say "I don't know based on the provided documents."
                
                Context:
                """ + context;

        return groqClient.chat(systemPrompt,question);
    }
}

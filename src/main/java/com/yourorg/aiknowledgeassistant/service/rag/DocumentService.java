package com.yourorg.aiknowledgeassistant.service.rag;

import com.yourorg.aiknowledgeassistant.domain.rag.Document;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentChunk;
import com.yourorg.aiknowledgeassistant.domain.user.User;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentChunkRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentService {

    private final EmbeddingClient embeddingClient;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final TextChunker textChunker;

    public DocumentService(EmbeddingClient embeddingClient,
                           DocumentChunkRepository documentChunkRepository,
                           DocumentRepository documentRepository,
                           UserRepository userRepository,
                           TextChunker textChunker) {
        this.embeddingClient = embeddingClient;
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.textChunker = textChunker;
    }

    @Transactional
    public void ingest(String filename, String content, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Document document = documentRepository.save(new Document(filename, user));

       List<String> chunks = textChunker.chunk(content);
       for(String chunk : chunks) {
           Float[] embedding = embeddingClient.embed(chunk);
           documentChunkRepository.save(new DocumentChunk(document, chunk,embedding));
       }
    }
}
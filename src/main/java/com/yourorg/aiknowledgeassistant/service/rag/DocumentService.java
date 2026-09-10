package com.yourorg.aiknowledgeassistant.service.rag;

import com.yourorg.aiknowledgeassistant.domain.rag.Document;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentChunk;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentStatus;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentType;
import com.yourorg.aiknowledgeassistant.domain.user.User;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentChunkRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final int MIN_EXTRACTED_CHARACTERS = 30;

    private final EmbeddingClient embeddingClient;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final TextChunker textChunker;
    private final FileParser fileParser;

    public DocumentService(EmbeddingClient embeddingClient,
                           DocumentChunkRepository documentChunkRepository,
                           DocumentRepository documentRepository,
                           UserRepository userRepository,
                           TextChunker textChunker,
                           FileParser fileParser) {
        this.embeddingClient = embeddingClient;
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.textChunker = textChunker;
        this.fileParser = fileParser;
    }

    @Transactional
    public Document submit(MultipartFile file, Long userId, DocumentType documentType) throws IOException {
        fileParser.validate(file);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return documentRepository.save(new Document(
                file.getOriginalFilename(), user, documentType, file.getBytes()));
    }

    /** Work is persisted before processing, so interrupted uploads resume after an app restart. */
    @Scheduled(initialDelay = 2_000, fixedDelay = 3_000)
    @Transactional
    public void processQueuedDocuments() {
        List<Document> pending = documentRepository.findTop10ByProcessingStatusInOrderByUploadedAtAsc(List.of(
                DocumentStatus.QUEUED, DocumentStatus.PARSING, DocumentStatus.EMBEDDING));
        for (Document document : pending) {
            process(document);
        }
    }

    private void process(Document document) {
        try {
            document.markParsing();
            ParsedDocument parsed = fileParser.parse(document.getSourceContent(), document.getFilename());
            String content = parsed.content() == null ? "" : parsed.content().trim();
            if (content.length() < MIN_EXTRACTED_CHARACTERS) {
                throw new IllegalArgumentException("No readable text was found. This may be a scanned PDF; OCR is not available yet.");
            }

            document.markEmbedding(new Document.ParsedExtraction(content, parsed.pageCount()));
            documentChunkRepository.deleteByDocument_Id(document.getId());
            List<String> chunks = textChunker.chunk(content);
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("The extracted text could not be split into searchable content");
            }

            for (int index = 0; index < chunks.size(); index++) {
                Float[] embedding = embeddingClient.embed(chunks.get(index));
                documentChunkRepository.save(new DocumentChunk(document, chunks.get(index), embedding, index));
            }
            document.markReady(chunks.size());
        } catch (Exception exception) {
            log.warn("Document processing failed for document {}: {}", document.getId(), exception.getMessage(), exception);
            documentChunkRepository.deleteByDocument_Id(document.getId());
            document.markFailed(publicFailureMessage(exception));
        }
    }

    private String publicFailureMessage(Exception exception) {
        if (exception instanceof IllegalArgumentException) {
            return exception.getMessage();
        }
        return "Document processing could not be completed. Please retry shortly.";
    }
}

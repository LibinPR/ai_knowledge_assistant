package com.yourorg.aiknowledgeassistant.service.rag;

import com.yourorg.aiknowledgeassistant.domain.rag.Document;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentChunk;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentStatus;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentType;
import com.yourorg.aiknowledgeassistant.domain.user.User;
import com.yourorg.aiknowledgeassistant.domain.workspace.Workspace;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentChunkRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserRepository;
import com.yourorg.aiknowledgeassistant.repository.workspace.WorkspaceRepository;
import com.yourorg.aiknowledgeassistant.service.workspace.WorkspaceAccessService;
import com.yourorg.aiknowledgeassistant.domain.workspace.WorkspaceMembershipRole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
    private final WorkspaceRepository workspaceRepository;
    private final TransactionTemplate transactions;
    private final WorkspaceAccessService workspaceAccessService;

    public DocumentService(EmbeddingClient embeddingClient,
                           DocumentChunkRepository documentChunkRepository,
                           DocumentRepository documentRepository,
                           UserRepository userRepository,
                           TextChunker textChunker,
                           FileParser fileParser,
                           WorkspaceRepository workspaceRepository,
                           PlatformTransactionManager transactionManager,
                           WorkspaceAccessService workspaceAccessService) {
        this.embeddingClient = embeddingClient;
        this.documentChunkRepository = documentChunkRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.textChunker = textChunker;
        this.fileParser = fileParser;
        this.workspaceRepository = workspaceRepository;
        this.transactions = new TransactionTemplate(transactionManager);
        this.workspaceAccessService = workspaceAccessService;
    }

    public Document submit(MultipartFile file, Long userId, DocumentType documentType) throws IOException {
        fileParser.validate(file);
        byte[] source = file.getBytes();
        return transactions.execute(status -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            Workspace workspace = workspaceRepository.findBySlug(personalWorkspaceSlug(userId))
                    .orElseThrow(() -> new IllegalStateException("Personal workspace not found for user"));
            return documentRepository.save(new Document(
                    file.getOriginalFilename(), user, workspace, documentType, source));
        });
    }

    public Document submitToWorkspace(
        MultipartFile file,
        Long userId,
        Long workspaceId,
        DocumentType documentType) throws IOException {

        workspaceAccessService.requireRole(
                workspaceId,
                userId,
                WorkspaceMembershipRole.MEMBER);

        fileParser.validate(file);
        byte[] source = file.getBytes();

        return transactions.execute(status -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new IllegalArgumentException("User not found"));

            Workspace workspace = workspaceRepository.findById(workspaceId)
                    .orElseThrow(() ->
                            new IllegalArgumentException("Workspace not found"));

            return documentRepository.save(new Document(
                    file.getOriginalFilename(),
                    user,
                    workspace,
                    documentType,
                    source));
        });
    }

    /** Work is persisted before processing, so interrupted uploads resume after an app restart. */
    @Scheduled(initialDelay = 2_000, fixedDelay = 3_000)
    public void processQueuedDocuments() {
        List<Long> pendingIds = transactions.execute(status -> documentRepository
                .findTop10ByProcessingStatusInOrderByUploadedAtAsc(List.of(
                        DocumentStatus.QUEUED, DocumentStatus.PARSING, DocumentStatus.EMBEDDING))
                .stream().map(Document::getId).toList());
        for (Long documentId : pendingIds) {
            process(documentId);
        }
    }

    private void process(Long documentId) {
        ProcessingInput input = transactions.execute(status -> {
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found"));
            document.markParsing();
            documentRepository.save(document);
            return new ProcessingInput(document.getId(), document.getFilename(), document.getSourceContent());
        });
        try {
            ParsedDocument parsed = fileParser.parse(input.sourceContent(), input.filename());
            String content = parsed.content() == null ? "" : parsed.content().trim();
            if (content.length() < MIN_EXTRACTED_CHARACTERS) {
                throw new IllegalArgumentException("No readable text was found. This may be a scanned PDF; OCR is not available yet.");
            }

            transactions.execute(status -> {
                Document document = documentRepository.findById(documentId).orElseThrow();
                document.markEmbedding(new Document.ParsedExtraction(content, parsed.pageCount()));
                documentRepository.save(document);
                documentChunkRepository.deleteByDocumentId(documentId);
                return null;
            });
            List<TextChunker.Chunk> chunks = textChunker.chunkWithMetadata(content, parsed.pageCount());
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("The extracted text could not be split into searchable content");
            }

            for (int index = 0; index < chunks.size(); index++) {
                TextChunker.Chunk chunk = chunks.get(index);
                Float[] embedding = embeddingClient.embed(chunk.content());
                transactions.execute(status -> {
                    Document document = documentRepository.getReferenceById(documentId);
                    documentChunkRepository.save(new DocumentChunk(document, chunk.content(), embedding,
                            chunk.index(), chunk.pageNumber(), chunk.sectionTitle()));
                    return null;
                });
            }
            transactions.execute(status -> {
                Document document = documentRepository.findById(documentId).orElseThrow();
                document.markReady(chunks.size());
                documentRepository.save(document);
                return null;
            });
        } catch (Exception exception) {
            log.warn("Document processing failed for document {}: {}", documentId, exception.getMessage(), exception);
            transactions.execute(status -> {
                    documentChunkRepository.deleteByDocumentId(documentId);
                documentRepository.findById(documentId).ifPresent(document -> {
                    document.markFailed(publicFailureMessage(exception));
                    documentRepository.save(document);
                });
                return null;
            });
        }
    }

    private record ProcessingInput(Long documentId, String filename, byte[] sourceContent) {}

    private String personalWorkspaceSlug(Long userId) {
        return "personal-" + userId;
    }

    private String publicFailureMessage(Exception exception) {
        if (exception instanceof IllegalArgumentException) {
            return exception.getMessage();
        }
        return "Document processing could not be completed. Please retry shortly.";
    }
}

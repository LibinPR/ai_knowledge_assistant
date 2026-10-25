package com.yourorg.aiknowledgeassistant.service.career;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.aiknowledgeassistant.domain.career.CareerMatch;
import com.yourorg.aiknowledgeassistant.domain.rag.Document;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentChunk;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentStatus;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentType;
import com.yourorg.aiknowledgeassistant.dto.CareerMatchResponse;
import com.yourorg.aiknowledgeassistant.dto.CreateCareerMatchRequest;
import com.yourorg.aiknowledgeassistant.repository.career.CareerMatchRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentChunkRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CareerMatchService {
    private static final Logger log = LoggerFactory.getLogger(CareerMatchService.class);
    private static final int MAX_CONTEXT_CHARS = 12_000;

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final CareerMatchRepository matchRepository;
    private final com.yourorg.aiknowledgeassistant.service.rag.GroqClient groqClient;
    private final ObjectMapper objectMapper;

    public CareerMatchService(DocumentRepository documentRepository,
                              DocumentChunkRepository chunkRepository,
                              CareerMatchRepository matchRepository,
                              com.yourorg.aiknowledgeassistant.service.rag.GroqClient groqClient,
                              ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.matchRepository = matchRepository;
        this.groqClient = groqClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CareerMatchResponse create(CreateCareerMatchRequest request, Long userId) {
        Document resume = ownedReadyDocument(request.resumeDocumentId(), userId, DocumentType.RESUME);
        Document job = ownedReadyDocument(request.jobDescriptionDocumentId(), userId, DocumentType.JOB_DESCRIPTION);
        CareerMatch match = matchRepository.save(new CareerMatch(resume.getUploadedBy(), resume, job));
        try {
            String prompt = buildPrompt(resume, job);
            JsonNode report = validateReport(groqClient.chat(SYSTEM_PROMPT, prompt));
            match.markReady(objectMapper.writeValueAsString(report));
        } catch (Exception ex) {
            log.error("Career match generation failed for match {}", match.getId(), ex);
            match.markFailed("Report generation failed. Please retry shortly.");
        }
        return CareerMatchResponse.from(matchRepository.save(match), objectMapper);
    }

    @Transactional(readOnly = true)
    public CareerMatchResponse get(Long id, Long userId) {
        CareerMatch match = matchRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Career match not found"));
        if (!match.getOwner().getId().equals(userId)) throw new SecurityException("You do not have access to this career match");
        return CareerMatchResponse.from(match, objectMapper);
    }

    @Transactional(readOnly = true)
    public List<CareerMatchResponse> list(Long userId) {
        return matchRepository.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
                .map(match -> CareerMatchResponse.from(match, objectMapper)).toList();
    }

    private Document ownedReadyDocument(Long id, Long userId, DocumentType expectedType) {
        Document document = documentRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Document not found"));
        if (!document.getUploadedBy().getId().equals(userId)) throw new SecurityException("You do not have access to this document");
        if (document.getDocumentType() != expectedType) throw new IllegalArgumentException("Document " + id + " must be labeled " + expectedType);
        if (document.getProcessingStatus() != DocumentStatus.READY) throw new IllegalArgumentException("Document " + id + " is not ready yet");
        return document;
    }

    private String buildPrompt(Document resume, Document job) {
        return "RESUME EVIDENCE:\n" + evidence(resume) + "\n\nJOB DESCRIPTION EVIDENCE:\n" + evidence(job);
    }

    private String evidence(Document document) {
        StringBuilder result = new StringBuilder();
        for (DocumentChunk chunk : chunkRepository.findByDocument_IdOrderByChunkIndexAsc(document.getId())) {
            String entry = "[sourceId=" + chunk.getId() + ", page=" + chunk.getPageNumber() + ", section=" + chunk.getSectionTitle() + "]\n" + chunk.getContent() + "\n\n";
            if (result.length() + entry.length() > MAX_CONTEXT_CHARS) break;
            result.append(entry);
        }
        return result.toString();
    }

    JsonNode validateReport(String raw) throws Exception {
        String cleaned = raw == null ? "" : raw.trim().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        JsonNode root = objectMapper.readTree(cleaned);
        if (root == null || !root.isObject() || !root.path("summary").isTextual() || root.path("summary").asText().isBlank() || !root.has("overallFit")
                || !root.get("overallFit").isNumber() || !root.has("strengths") || !root.get("strengths").isArray()
                || !root.has("gaps") || !root.get("gaps").isArray() || !root.has("recommendations") || !root.get("recommendations").isArray()) {
            throw new IllegalArgumentException("AI returned an invalid match report");
        }
        int score = root.get("overallFit").asInt(-1);
        if (score < 0 || score > 100) throw new IllegalArgumentException("AI returned an invalid fit score");
        return root;
    }

    private static final String SYSTEM_PROMPT = """
            You are a rigorous hiring analyst. Compare the resume evidence to the job description evidence.
            Return ONLY valid JSON (no markdown) with exactly these keys:
            summary (string), overallFit (integer 0-100), strengths (array of objects with requirement, evidence, sourceIds),
            gaps (array of objects with requirement, reason, sourceIds), recommendations (array of objects with action, why).
            Never invent experience. Use sourceIds from the supplied evidence and keep the analysis concise and actionable.
            """;
}

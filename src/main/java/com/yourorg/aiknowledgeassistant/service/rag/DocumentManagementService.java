package com.yourorg.aiknowledgeassistant.service.rag;

import com.yourorg.aiknowledgeassistant.repository.rag.DocumentRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import com.yourorg.aiknowledgeassistant.domain.rag.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentManagementService {
  private final DocumentRepository documentRepository;

  public DocumentManagementService(DocumentRepository documentRepository) {
    this.documentRepository = documentRepository;
  }

  public List<Document> listDocuments(Long userId, boolean isAdmin) {
    if (isAdmin) {
      return documentRepository.findAll();
    }
    return documentRepository.findByUploadedById(userId);
  }

  public Document getDocument(Long documentId, Long userId, boolean isAdmin) {
    Document document = documentRepository.findById(documentId)
        .orElseThrow(() -> new EntityNotFoundException("Document not found"));
    if (!isAdmin && !document.getUploadedBy().getId().equals(userId)) {
      throw new SecurityException("You do not have permission to view this document");
    }
    return document;
  }

  @Transactional
  public void deleteDocument(Long documentId, Long userId, boolean isAdmin) {
    Document document = getDocument(documentId, userId, isAdmin);
    documentRepository.delete(document);
  }
}

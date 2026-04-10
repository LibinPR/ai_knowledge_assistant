package com.yourorg.aiknowledgeassistant.service.rag;

import com.yourorg.aiknowledgeassistant.repository.rag.DocumentRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import com.yourorg.aiknowledgeassistant.domain.rag.Document;

import java.util.List;

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

  @Transactional
  public void deleteDocument(Long documentId, Long userId, boolean isAdmin) {
    Document document = documentRepository.findById(documentId)
        .orElseThrow(() -> new EntityNotFoundException("Document not found"));

    if (!isAdmin && !document.getUploadedBy().getId().equals(userId)) {
      throw new SecurityException("You do not have permission to delete this document");
    }
    documentRepository.delete(document);
  }
}

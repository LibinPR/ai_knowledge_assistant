package com.yourorg.aiknowledgeassistant.repository.rag;

import com.yourorg.aiknowledgeassistant.domain.rag.Document;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
  List<Document> findByUploadedById(Long userId);
  List<Document> findTop10ByProcessingStatusInOrderByUploadedAtAsc(List<DocumentStatus> processingStatuses);
  long countByUploadedByIdAndProcessingStatusIn(Long userId, List<DocumentStatus> processingStatuses);
  long count();
}

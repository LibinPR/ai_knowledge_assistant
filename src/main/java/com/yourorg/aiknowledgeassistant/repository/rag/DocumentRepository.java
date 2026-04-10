package com.yourorg.aiknowledgeassistant.repository.rag;

import com.yourorg.aiknowledgeassistant.domain.rag.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
  List<Document> findByUploadedById(Long userId);
}

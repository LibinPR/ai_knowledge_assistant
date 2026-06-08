package com.yourorg.aiknowledgeassistant.service.admin;

import com.yourorg.aiknowledgeassistant.repository.rag.DocumentChunkRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentRepository;
import com.yourorg.aiknowledgeassistant.repository.user.UserRepository;
import com.yourorg.aiknowledgeassistant.dto.AdminStatsResponse;

import org.springframework.stereotype.Service;

@Service
public class AdminService {
  private final UserRepository userRepository;
  private final DocumentRepository documentRepository;
  private final DocumentChunkRepository chunkRepository;


  public AdminService(
    UserRepository userRepository,
    DocumentRepository documentRepository,
    DocumentChunkRepository chunkRepository
  ) {
    this.userRepository = userRepository;
    this.documentRepository = documentRepository;
    this.chunkRepository = chunkRepository;
  }

  public AdminStatsResponse getStats() {
    return new AdminStatsResponse(
      userRepository.count(),
      documentRepository.count(),
      chunkRepository.count()
    );
  }
}
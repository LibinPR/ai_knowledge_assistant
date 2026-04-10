package com.yourorg.aiknowledgeassistant.repository.rag;

import com.yourorg.aiknowledgeassistant.domain.rag.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

        @Query(value = """
                        SELECT id, content FROM document_chunks
                        ORDER BY embedding <=> CAST(:embedding AS vector)
                        LIMIT :topK
                        """, nativeQuery = true)
        List<DocumentChunkSummary> findSimilar(
                        @Param("embedding") String embedding,
                        @Param("topK") int topK);
}

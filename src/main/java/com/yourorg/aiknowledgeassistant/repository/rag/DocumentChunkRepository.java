package com.yourorg.aiknowledgeassistant.repository.rag;

import com.yourorg.aiknowledgeassistant.repository.rag.SearchResultProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentChunk;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

        @Query(value = """
                SELECT
                        d.id AS documentId,
                        d.filename AS documentName,
                        dc.content AS content,
                        1 - (dc.embedding <=> CAST(:embedding AS vector)) AS similarity
                FROM document_chunks dc
                JOIN documents d
                        ON dc.document_id = d.id
                WHERE d.uploaded_by = :userId
                ORDER BY dc.embedding <=> CAST(:embedding AS vector)
                LIMIT :topK
                """, nativeQuery = true)
        List<SearchResultProjection> findSimilar(
                @Param("embedding") String embedding,
                @Param("userId") Long userId,
                @Param("topK") int topK);

        void deleteByDocument_Id(Long documentId);

        long count();
}

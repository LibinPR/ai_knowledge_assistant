package com.yourorg.aiknowledgeassistant.repository.rag;

import com.yourorg.aiknowledgeassistant.repository.rag.SearchResultProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.yourorg.aiknowledgeassistant.domain.rag.DocumentChunk;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

        @Query(value = """
                SELECT
                        dc.id AS chunkId,
                        d.id AS documentId,
                        d.filename AS documentName,
                        dc.content AS content,
                        dc.page_number AS pageNumber,
                        dc.section_title AS sectionTitle,
                        1 - (dc.embedding <=> CAST(:embedding AS vector)) AS similarity,
                        0.0 AS keywordScore
                FROM document_chunks dc
                JOIN documents d
                        ON dc.document_id = d.id
                WHERE d.uploaded_by = :userId
                  AND d.processing_status = 'READY'
                  AND dc.embedding IS NOT NULL
                ORDER BY dc.embedding <=> CAST(:embedding AS vector)
                LIMIT :topK
                """, nativeQuery = true)
        List<SearchResultProjection> findSimilar(
                @Param("embedding") String embedding,
                @Param("userId") Long userId,
                @Param("topK") int topK);

        @Query(value = """
                SELECT
                        dc.id AS chunkId,
                        d.id AS documentId,
                        d.filename AS documentName,
                        dc.content AS content,
                        dc.page_number AS pageNumber,
                        dc.section_title AS sectionTitle,
                        0.0 AS similarity,
                        ts_rank_cd(dc.search_vector, plainto_tsquery('simple', :question)) AS keywordScore
                FROM document_chunks dc
                JOIN documents d ON dc.document_id = d.id
                WHERE d.uploaded_by = :userId
                  AND d.processing_status = 'READY'
                  AND dc.search_vector @@ plainto_tsquery('simple', :question)
                ORDER BY keywordScore DESC
                LIMIT :topK
                """, nativeQuery = true)
        List<SearchResultProjection> findKeywordMatches(
                @Param("question") String question,
                @Param("userId") Long userId,
                @Param("topK") int topK);

        
@Query(value = """
        SELECT
                dc.id AS chunkId,
                d.id AS documentId,
                d.filename AS documentName,
                dc.content AS content,
                dc.page_number AS pageNumber,
                dc.section_title AS sectionTitle,
                1 - (dc.embedding <=> CAST(:embedding AS vector)) AS similarity,
                0.0 AS keywordScore
        FROM document_chunks dc
        JOIN documents d ON dc.document_id = d.id
        WHERE d.workspace_id = :workspaceId
          AND d.processing_status = 'READY'
          AND dc.embedding IS NOT NULL
        ORDER BY dc.embedding <=> CAST(:embedding AS vector)
        LIMIT :topK
        """, nativeQuery = true)
List<SearchResultProjection> findSimilarInWorkspace(
        @Param("embedding") String embedding,
        @Param("workspaceId") Long workspaceId,
        @Param("topK") int topK);

        @Query(value = """
                SELECT
                        dc.id AS chunkId,
                        d.id AS documentId,
                        d.filename AS documentName,
                        dc.content AS content,
                        dc.page_number AS pageNumber,
                        dc.section_title AS sectionTitle,
                        0.0 AS similarity,
                        ts_rank_cd(
                        dc.search_vector,
                        plainto_tsquery('simple', :question)
                        ) AS keywordScore
                FROM document_chunks dc
                JOIN documents d ON dc.document_id = d.id
                WHERE d.workspace_id = :workspaceId
                AND d.processing_status = 'READY'
                AND dc.search_vector @@ plainto_tsquery('simple', :question)
                ORDER BY keywordScore DESC
                LIMIT :topK
                """, nativeQuery = true)
        List<SearchResultProjection> findKeywordMatchesInWorkspace(
                @Param("question") String question,
                @Param("workspaceId") Long workspaceId,
                @Param("topK") int topK);


        @Modifying
        @Query(value = "DELETE FROM document_chunks WHERE document_id = :documentId", nativeQuery = true)
        int deleteByDocumentId(@Param("documentId") Long documentId);

        List<DocumentChunk> findByDocument_IdOrderByChunkIndexAsc(Long documentId);

        long count();
}

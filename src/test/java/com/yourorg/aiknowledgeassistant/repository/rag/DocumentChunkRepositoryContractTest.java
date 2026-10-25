package com.yourorg.aiknowledgeassistant.repository.rag;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkRepositoryContractTest {

    @Test
    void vectorAndKeywordQueriesAreScopedToTheOwnerAndReadyDocuments() throws Exception {
        assertQueryScopesToOwnerAndReadyStatus("findSimilar", String.class, Long.class, int.class);
        assertQueryScopesToOwnerAndReadyStatus("findKeywordMatches", String.class, Long.class, int.class);
    }

    private void assertQueryScopesToOwnerAndReadyStatus(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = DocumentChunkRepository.class.getMethod(methodName, parameterTypes);
        Query query = method.getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value())
                .contains("d.uploaded_by = :userId")
                .contains("d.processing_status = 'READY'");
    }
}

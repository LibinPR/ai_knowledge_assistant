package com.yourorg.aiknowledgeassistant.service.career;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.aiknowledgeassistant.repository.career.CareerMatchRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentChunkRepository;
import com.yourorg.aiknowledgeassistant.repository.rag.DocumentRepository;
import com.yourorg.aiknowledgeassistant.service.rag.GroqClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CareerMatchServiceTest {

    private final CareerMatchService service = new CareerMatchService(
            mock(DocumentRepository.class),
            mock(DocumentChunkRepository.class),
            mock(CareerMatchRepository.class),
            mock(GroqClient.class),
            new ObjectMapper());

    @Test
    void acceptsTheRequiredStructuredReportIncludingFencedJson() throws Exception {
        String report = """
                ```json
                {
                  "summary": "Strong backend fit.",
                  "overallFit": 82,
                  "strengths": [{"requirement": "Java", "evidence": "Built APIs", "sourceIds": [1]}],
                  "gaps": [],
                  "recommendations": [{"action": "Show system design work", "why": "Make architecture experience explicit"}]
                }
                ```
                """;

        assertThat(service.validateReport(report).path("overallFit").asInt()).isEqualTo(82);
    }

    @Test
    void rejectsReportsWithAnInvalidFitScoreOrMissingRequiredFields() {
        assertThatThrownBy(() -> service.validateReport("""
                {"summary":"Too high","overallFit":101,"strengths":[],"gaps":[],"recommendations":[]}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid fit score");

        assertThatThrownBy(() -> service.validateReport("{" + "\"summary\":\"Missing arrays\",\"overallFit\":50}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid match report");
    }
}

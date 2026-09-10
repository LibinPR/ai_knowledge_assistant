package com.yourorg.aiknowledgeassistant.service.rag;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileParserTest {
    private final FileParser fileParser = new FileParser();

    @Test
    void parsesUtf8TextWithoutChangingContent() throws Exception {
        ParsedDocument parsed = fileParser.parse(
                "Experience: built reliable RAG services.".getBytes(StandardCharsets.UTF_8), "resume.txt");

        assertThat(parsed.content()).contains("reliable RAG services");
        assertThat(parsed.pageCount()).isNull();
    }

    @Test
    void rejectsAnUnsupportedUploadBeforeItIsQueued() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.exe", "application/octet-stream", new byte[]{1});

        assertThatThrownBy(() -> fileParser.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    void rejectsEmptySourceContent() {
        assertThatThrownBy(() -> fileParser.parse(new byte[0], "resume.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }
}

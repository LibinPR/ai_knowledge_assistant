package com.yourorg.aiknowledgeassistant.service.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {
    private final TextChunker chunker = new TextChunker();

    @Test
    void keepsSectionAndPageMetadataOnChunks() {
        String experience = "Experience\n" + "Built reliable backend services. ".repeat(30);
        String projects = "\fProjects\n" + "Created an evidence-grounded assistant. ".repeat(30);

        List<TextChunker.Chunk> chunks = chunker.chunkWithMetadata(experience + projects, 2);

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).anyMatch(chunk -> chunk.sectionTitle().equals("Experience") && chunk.pageNumber() == 1);
        assertThat(chunks).anyMatch(chunk -> chunk.sectionTitle().equals("Projects") && chunk.pageNumber() == 2);
        assertThat(chunks).extracting(TextChunker.Chunk::index).isSorted();
    }
}

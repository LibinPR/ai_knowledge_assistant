package com.yourorg.aiknowledgeassistant.service.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Set;

@Component
public class TextChunker {

    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 100;

    public List<String> chunk(String text) {
        return chunkWithMetadata(text, null).stream().map(Chunk::content).toList();
    }

    public List<Chunk> chunkWithMetadata(String text, Integer pageCount) {
        List<Chunk> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        int length = text.length();
        int start = 0;

        while (start < length) {
            int end = Math.min(start + CHUNK_SIZE, length);

            if (end < length) {
                int lastPeriod = text.lastIndexOf('.', end);
                if (lastPeriod > start + OVERLAP) {
                    end = lastPeriod + 1;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isBlank()) {
                int pageNumber = pageCount == null ? 1 : pageNumberAt(text, start, pageCount);
                chunks.add(new Chunk(chunk, chunks.size(), pageNumber, sectionAt(text, start)));
            }

            // FIXED: if we reached the end, stop - this bug resulted in OOM
            if (end >= length)
                break;

            start = end - OVERLAP;
        }

        return chunks;
    }

    private int pageNumberAt(String text, int offset, int pageCount) {
        int page = 1;
        for (int index = 0; index < offset && index < text.length(); index++) {
            if (text.charAt(index) == '\f') page++;
        }
        return Math.min(page, Math.max(pageCount, 1));
    }

    private String sectionAt(String text, int offset) {
        String before = text.substring(0, Math.min(offset, text.length()));
        String[] lines = before.split("\\R");
        int lastCompleteLine = before.endsWith("\n") || before.endsWith("\r")
                ? lines.length - 1 : lines.length - 2;
        for (int index = lastCompleteLine; index >= Math.max(0, lastCompleteLine - 39); index--) {
            String candidate = lines[index].trim();
            String normalized = candidate.toLowerCase(Locale.ROOT).replaceAll("[^a-z ]", "").trim();
            if (candidate.startsWith("#") || KNOWN_SECTIONS.contains(normalized)) {
                return candidate.replaceFirst("^#+\\s*", "");
            }
        }
        return "General";
    }

    private static final Set<String> KNOWN_SECTIONS = Set.of(
            "summary", "profile", "experience", "work experience", "employment",
            "projects", "skills", "technical skills", "education", "certifications",
            "requirements", "qualifications", "responsibilities", "preferred qualifications",
            "benefits", "about the role", "tech stack");

    public record Chunk(String content, int index, int pageNumber, String sectionTitle) {}
}

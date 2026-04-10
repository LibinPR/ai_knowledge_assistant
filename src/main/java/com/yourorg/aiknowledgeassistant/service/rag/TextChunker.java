package com.yourorg.aiknowledgeassistant.service.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 100;

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();

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
                chunks.add(chunk);
            }

            // FIXED: if we reached the end, stop - this bug resulted in OOM
            if (end >= length)
                break;

            start = end - OVERLAP;
        }

        return chunks;
    }
}
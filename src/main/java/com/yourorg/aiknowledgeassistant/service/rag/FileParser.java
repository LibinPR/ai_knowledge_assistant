package com.yourorg.aiknowledgeassistant.service.rag;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class FileParser {

    public String parse(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if(filename == null) {
            throw new IllegalArgumentException("File has no name");
        }

        if(filename.endsWith(".pdf")) {
            try(PDDocument doc = Loader.loadPDF(file.getBytes())) {
                return new PDFTextStripper().getText(doc);
            }
        } else if (filename.endsWith(".txt")) {
            return new String(file.getBytes());
        } else {
            throw new IllegalArgumentException("Unsupported file type . Only PDF and TXT files allowed");
        }
    }
}

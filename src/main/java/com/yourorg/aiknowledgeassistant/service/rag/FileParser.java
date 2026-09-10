package com.yourorg.aiknowledgeassistant.service.rag;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class FileParser {
    public void validate(MultipartFile file) {
        long maxSize = 10*1024*1024;     //10MB
        if(file.getSize()>maxSize) {
            throw new IllegalArgumentException("File size exceeds the 10MB limit");
        }

        String filename = file.getOriginalFilename();
        if(filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File name cannot be Empty");
        }

        filename = filename.toLowerCase();

        if (!filename.endsWith(".pdf") && !filename.endsWith(".txt")
                && !filename.endsWith(".docx") && !filename.endsWith(".md")) {
            throw new IllegalArgumentException("Unsupported file type. Allowed: PDF, TXT, MD, DOCX");
        }
    }

    public ParsedDocument parse(byte[] bytes, String filename) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("The uploaded file is empty");
        }
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }
        filename = filename.toLowerCase();

        if(filename.endsWith(".pdf")) {
            try(PDDocument doc = Loader.loadPDF(bytes)) {
                return new ParsedDocument(new PDFTextStripper().getText(doc), doc.getNumberOfPages());
            }
        } else if (filename.endsWith(".txt")) {
            return new ParsedDocument(new String(bytes, StandardCharsets.UTF_8), null);
        } else if(filename.endsWith(".docx")) {
            try(XWPFDocument doc = new XWPFDocument(new java.io.ByteArrayInputStream(bytes))) {
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
                return new ParsedDocument(extractor.getText(), null);
            }
        }else if(filename.endsWith(".md")) {
            return new ParsedDocument(new String(bytes, StandardCharsets.UTF_8), null);
        }else{
            throw new IllegalArgumentException("Unsupported file type. Allowed: PDF, TXT, MD, DOCX");
        }
    }
}

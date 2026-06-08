package com.yourorg.aiknowledgeassistant.service.rag;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

import java.io.IOException;

@Component
public class FileParser {
    public String parse(MultipartFile file) throws IOException {
        
        long maxSize = 10*1024*1024;     //10MB
        if(file.getSize()>maxSize) {
            throw new IllegalArgumentException("File size exceeds the 10MB limit");
        }

        String filename = file.getOriginalFilename();
        if(filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File name cannot be Empty");
        }

        filename = filename.toLowerCase();

        if(filename.endsWith(".pdf")) {
            try(PDDocument doc = Loader.loadPDF(file.getBytes())) {
                return new PDFTextStripper().getText(doc);
            }
        } else if (filename.endsWith(".txt")) {
            return new String(file.getBytes());
        } else if(filename.endsWith(".docx")) {
            try(XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
                return extractor.getText();
            }
        }else if(filename.endsWith(".md")) {
            return new String(file.getBytes());
        }else{
            throw new IllegalArgumentException("Unsupported file type . Allowed : PDF , TXT , MD , DOCX");
        }
    }
}

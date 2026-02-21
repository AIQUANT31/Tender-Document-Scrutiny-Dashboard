package com.example.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class PdfTextExtractor {

    private static final Logger logger = LoggerFactory.getLogger(PdfTextExtractor.class);

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "Error: Empty file";
        }
        
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equals("application/pdf")) {
            return "Error: Not a PDF file";
        }
        
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            logger.info("Extracted {} characters from PDF", text.length());
            return text;
            
        } catch (IOException e) {
            logger.error("Error extracting PDF text: ", e);
            return "Error: " + e.getMessage();
        }
    }

    
    public java.util.Map<String, String> extractTextFromMultiple(java.util.List<MultipartFile> files) {
        java.util.Map<String, String> results = new java.util.HashMap<>();
        
        if (files == null) {
            return results;
        }
        
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String fileName = file.getOriginalFilename();
                results.put(fileName != null ? fileName : "unknown", extractText(file));
            }
        }
        
        return results;
    }
}

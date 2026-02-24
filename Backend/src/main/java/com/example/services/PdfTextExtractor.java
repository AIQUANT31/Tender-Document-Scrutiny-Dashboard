package com.example.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * PDF Text Extractor using OCR only (no PDFBox text extraction)
 * This service delegates to OcrService for text extraction
 */
@Service
public class PdfTextExtractor {

    private static final Logger logger = LoggerFactory.getLogger(PdfTextExtractor.class);
    
    private final OcrService ocrService;

    public PdfTextExtractor(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "Error: Empty file";
        }
        
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equals("application/pdf")) {
            return "Error: Not a PDF file";
        }
        
        try {
            // Use OCR service for text extraction (no PDFBox text extraction)
            String text = ocrService.extractText(file);
            
            logger.info("Extracted {} characters from PDF using OCR", text.length());
            return text;
            
        } catch (Exception e) {
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

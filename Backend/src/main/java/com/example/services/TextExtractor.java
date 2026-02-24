package com.example.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class TextExtractor {

    private static final Logger logger = LoggerFactory.getLogger(TextExtractor.class);

    @Autowired
    private OcrService ocrService;

    public Map<String, String> extractTextFromFiles(MultipartFile[] files) {

        Map<String, String> extracted = new HashMap<>();

        for (MultipartFile file : files) {

            if (file == null || file.isEmpty())
                continue;

            String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("unknown");

            try {
                logger.info("Extracting text from: {}", fileName);

                String content = ocrService.extractText(file);

                if (content == null || content.isBlank()) {
                    extracted.put(fileName, "IMAGE_PDF_FALLBACK");
                    logger.warn("OCR failed for {}", fileName);
                } else {
                    extracted.put(fileName, content);
                    logger.info("✓ OCR Success: {}", fileName);
                }

            } catch (Exception e) {
                extracted.put(fileName, "IMAGE_PDF_FALLBACK");
                logger.error("OCR Exception for {}: {}", fileName, e.getMessage());
            }
        }

        return extracted;
    }

    public boolean isValidContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        if (content.toLowerCase().startsWith("error")) {
            return false;
        }
        if (content.equals("image_pdf_fallback")) {
            return false;
        }
        return true;
    }

    public String extractTextFromFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "IMAGE_PDF_FALLBACK";
        }

        String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("unknown");

        try {
            logger.info("Extracting text from: {}", fileName);
            String content = ocrService.extractText(file);

            if (content == null || content.isBlank()) {
                logger.warn("OCR failed for {}", fileName);
                return "IMAGE_PDF_FALLBACK";
            }

            logger.info("✓ OCR Success: {}", fileName);
            return content;

        } catch (Exception e) {
            logger.error("OCR Exception for {}: {}", fileName, e.getMessage());
            return "IMAGE_PDF_FALLBACK";
        }
    }
}

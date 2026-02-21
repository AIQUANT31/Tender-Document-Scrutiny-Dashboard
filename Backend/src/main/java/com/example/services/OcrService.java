package com.example.services;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * OCR Service using Tesseract to extract text from PDFs
 * Handles:
 * ✔ Text PDFs
 * ✔ Scanned PDFs
 * ✔ Image-converted PDFs
 */
@Service
public class OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    private final Tesseract tesseract;
    private String tessDataPath = null;

    public OcrService() {
        this.tesseract = new Tesseract();
        initializeTesseract();
    }

    private void initializeTesseract() {
        try {
            String[] possiblePaths = {
                    "/usr/share/tesseract-ocr/tessdata",
                    "/usr/share/tesseract-ocr/5/tessdata",
                    "/usr/share/tessdata",
                    "/usr/local/share/tessdata",
                    "tessdata",
                    "src/main/resources/tessdata"
            };

            for (String path : possiblePaths) {
                File dir = new File(path);
                if (dir.exists() && new File(path, "eng.traineddata").exists()) {
                    tessDataPath = path;
                    break;
                }
            }

            if (tessDataPath != null) {
                tesseract.setDatapath(tessDataPath);
                tesseract.setLanguage("eng");
                logger.info("Tesseract initialized with path: {}", tessDataPath);
            } else {
                logger.warn("Tesseract tessdata folder NOT found!");
            }

        } catch (Exception e) {
            logger.error("Tesseract initialization failed", e);
        }
    }

    /**
     * Public method used by validation logic
     * CRITICAL: Reads InputStream only once to avoid stream consumption issues
     */
    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "Error: Empty file";
        }

        try {
            // Read bytes ONCE and reuse for both PDF text extraction and OCR
            byte[] pdfBytes;
            try {
                pdfBytes = file.getInputStream().readAllBytes();
            } catch (IOException e) {
                logger.error("Error reading file: {}", e.getMessage());
                return "Error: Could not read file - " + e.getMessage();
            }
            
            if (pdfBytes == null || pdfBytes.length == 0) {
                logger.warn("File is empty: {}", file.getOriginalFilename());
                return "Error: Empty file";
            }
            
            logger.debug("Read {} bytes from file: {}", pdfBytes.length, file.getOriginalFilename());

            // Try to extract embedded text first
            String pdfText = extractPdfTextFromBytes(pdfBytes);
            logger.info("PDFBox Text Length: {}", pdfText != null ? pdfText.length() : 0);

            // If text is sufficient → return directly
            if (pdfText != null && pdfText.trim().length() > 30) {
                logger.info("Sufficient text extracted, skipping OCR");
                return pdfText;
            }

            // Otherwise → OCR fallback (use same bytes)
            logger.info("Running OCR fallback (text length: {})...", pdfText != null ? pdfText.length() : 0);
            String ocrText = extractTextWithOcrFromBytes(pdfBytes);
            logger.info("OCR Text Length: {}", ocrText != null ? ocrText.length() : 0);

            // Combine both results
            String combined = (pdfText != null ? pdfText : "") + " " + (ocrText != null ? ocrText : "");
            return combined.trim();

        } catch (Exception e) {
            logger.error("Text extraction failed: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Extract embedded text (text-based PDFs) from byte array
     */
    private String extractPdfTextFromBytes(byte[] pdfBytes) {
        try {
            if (pdfBytes == null || pdfBytes.length == 0) {
                logger.warn("PDF bytes are empty");
                return "";
            }
            
            logger.debug("Reading PDF: {} bytes", pdfBytes.length);
            
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                org.apache.pdfbox.text.PDFTextStripper stripper =
                        new org.apache.pdfbox.text.PDFTextStripper();

                String text = stripper.getText(document);
                logger.debug("Extracted {} characters of embedded text", text != null ? text.length() : 0);
                return text != null ? text : "";

            } catch (Exception e) {
                logger.debug("PDF text extraction failed (may be image-based PDF): {}", e.getMessage());
                return "";
            }

        } catch (Exception e) {
            logger.warn("Unexpected error extracting PDF text: {}", e.getMessage());
            return "";
        }
    }

    /**
     * OCR fallback using PDFRenderer (MOST RELIABLE)
     * This is especially important for image-converted PDFs
     * Uses byte array to avoid InputStream issues
     */
    private String extractTextWithOcrFromBytes(byte[] pdfBytes) {
        StringBuilder fullText = new StringBuilder();

        if (tessDataPath == null) {
            logger.warn("OCR skipped — Tesseract not initialized. Image-converted PDFs require OCR!");
            return "";
        }

        if (pdfBytes == null || pdfBytes.length == 0) {
            logger.warn("Cannot perform OCR - PDF bytes are empty");
            return "";
        }
        
        try {
            logger.info("Performing OCR on PDF: {} bytes, {} pages", pdfBytes.length, 
                getPageCount(pdfBytes));

            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFRenderer renderer = new PDFRenderer(document);
                int pageCount = document.getNumberOfPages();

                for (int page = 0; page < pageCount; page++) {
                    try {
                        logger.debug("Rendering page {} of {} for OCR", page + 1, pageCount);
                        BufferedImage image = renderer.renderImageWithDPI(page, 300);

                        String ocrResult = performOcr(image);

                        if (ocrResult != null && !ocrResult.trim().isEmpty()) {
                            fullText.append(ocrResult).append(" ");
                            logger.info("OCR extracted {} chars from page {}",
                                    ocrResult.length(), page + 1);
                        } else {
                            logger.debug("No text extracted from page {}", page + 1);
                        }
                    } catch (Exception e) {
                        logger.warn("Error processing page {}: {}", page + 1, e.getMessage());
                    }
                }

            } catch (Exception e) {
                logger.error("Error loading PDF for OCR: {}", e.getMessage(), e);
                return "";
            }

        } catch (Exception e) {
            logger.error("Unexpected error during OCR: {}", e.getMessage(), e);
            return "";
        }

        String result = fullText.toString().trim();
        logger.info("Total OCR text extracted: {} characters", result.length());
        return result;
    }
    
    /**
     * Get page count without loading full document (for logging)
     */
    private int getPageCount(byte[] pdfBytes) {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            return doc.getNumberOfPages();
        } catch (Exception e) {
            return -1; // Unknown
        }
    }

    /**
     * Perform OCR on image
     */
    private String performOcr(BufferedImage image) {
        try {
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            logger.error("OCR failed", e);
            return "";
        }
    }

    /**
     * Health check
     */
    public boolean isInitialized() {
        return tessDataPath != null;
    }
}
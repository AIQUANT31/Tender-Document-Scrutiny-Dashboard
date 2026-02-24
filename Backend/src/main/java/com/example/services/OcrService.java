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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;


@Service
public class OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024;
    private static final int SMALL_FILE_THRESHOLD = 5 * 1024 * 1024; 
    private static final int MEDIUM_DPI = 200;
    private static final int HIGH_DPI = 300;
    private static final int MAX_PAGES_PER_BATCH = 10; 
    private static final int MAX_IMAGE_DIMENSION = 4000; 

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
                    logger.info("Found Tesseract tessdata at: {}", path);
                    break;
                }
            }

            if (tessDataPath != null) {
                tesseract.setDatapath(tessDataPath);
                tesseract.setLanguage("eng");
                
                tesseract.setTessVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,()-/ ");
                tesseract.setTessVariable("preserve_interword_spaces", "1");
                logger.info("✓ Tesseract OCR initialized successfully with path: {}", tessDataPath);
            } else {
                logger.error("✗ Tesseract tessdata folder NOT found! OCR will not work.");
                logger.error("  Searched paths: /usr/share/tesseract-ocr/tessdata, /usr/share/tesseract-ocr/5/tessdata, etc.");
            }

        } catch (Exception e) {
            logger.error("✗ Tesseract initialization failed", e);
        }
    }

    
    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "Error: Empty file";
        }

        try {
            long fileSize = file.getSize();
            String originalFilename = file.getOriginalFilename();
            
            logger.info("Processing file: {} (Size: {} MB)", originalFilename, fileSize / (1024 * 1024));

        
            if (fileSize > MAX_FILE_SIZE) {
                logger.error("File too large: {} MB (max: {} MB)", fileSize / (1024 * 1024), MAX_FILE_SIZE / (1024 * 1024));
                return "Error: File too large - maximum size is 100MB";
            }

          
            if (fileSize <= SMALL_FILE_THRESHOLD) {
        
                return extractSmallFile(file);
            } else if (fileSize <= 30 * 1024 * 1024) {
                
                return extractMediumFile(file);
            } else {
            
                return extractLargeFile(file);
            }

        } catch (Exception e) {
            logger.error("Text extraction failed: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

  
    private String extractSmallFile(MultipartFile file) {
        try {
            byte[] pdfBytes = file.getBytes();
            
            // Always use OCR for small files (no embedded text extraction)
            logger.info("Using OCR for small file ({} DPI)", MEDIUM_DPI);
            return extractTextWithOcrFromBytes(pdfBytes, MEDIUM_DPI);
            
        } catch (IOException e) {
            logger.warn("Small file extraction failed: {}", e.getMessage());
            try {
                return extractTextWithOcrFromBytes(file.getBytes(), MEDIUM_DPI);
            } catch (IOException ex) {
                logger.error("Failed to read file bytes: {}", ex.getMessage());
                return "Error: Could not read file - " + ex.getMessage();
            }
        }
    }


    private String extractMediumFile(MultipartFile file) {
        try {
            byte[] pdfBytes = file.getBytes();
            
            // Always use OCR for medium files
            logger.info("Using optimized OCR for medium file ({} DPI)", HIGH_DPI);
            return extractTextWithOcrFromBytes(pdfBytes, HIGH_DPI);
            
        } catch (IOException e) {
            logger.warn("Medium file extraction failed: {}", e.getMessage());
            try {
                return extractTextWithOcrFromBytes(file.getBytes(), HIGH_DPI);
            } catch (IOException ex) {
                logger.error("Failed to read file bytes: {}", ex.getMessage());
                return "Error: Could not read file - " + ex.getMessage();
            }
        }
    }

    // Extract text from large files - batch processing with memory management
     
    private String extractLargeFile(MultipartFile file) {
        try {
            byte[] pdfBytes = file.getBytes();
            
            
            logger.info("Processing large file with batch OCR ({} MB)", pdfBytes.length / (1024 * 1024));
            
            return extractLargeFileWithBatchProcessing(pdfBytes);
            
        } catch (Exception e) {
            logger.error("Large file extraction failed: {}", e.getMessage(), e);
            return "Error: Could not process large file - " + e.getMessage();
        }
    }
    private String extractPdfTextFromBytes(byte[] pdfBytes) {
        // PDFBox text extraction disabled - using OCR only
        logger.info("Skipping embedded text extraction - using OCR only");
        return "";
    }

    
    private String extractTextWithOcrFromBytes(byte[] pdfBytes, int dpi) {
        StringBuilder fullText = new StringBuilder();

        if (tessDataPath == null) {
            logger.warn("OCR skipped — Tesseract not initialized");
            return "";
        }

        if (pdfBytes == null || pdfBytes.length == 0) {
            logger.warn("Cannot perform OCR - PDF bytes are empty");
            return "";
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            
            logger.info("Processing {} pages at {} DPI", pageCount, dpi);

            for (int page = 0; page < pageCount; page++) {
                try {
                    BufferedImage image = renderer.renderImageWithDPI(page, dpi);
                    
                    // Optimize image before OCR
                    BufferedImage optimizedImage = optimizeImageForOcr(image);
                    
                    String ocrResult = performOcr(optimizedImage);

                    if (ocrResult != null && !ocrResult.trim().isEmpty()) {
                        fullText.append(ocrResult).append(" ");
                        logger.debug("OCR extracted {} chars from page {}", ocrResult.length(), page + 1);
                    }
                    
                    // Clean up to free memory
                    image.flush();
                    optimizedImage.flush();
                    
                } catch (Exception e) {
                    logger.warn("Error processing page {}: {}", page + 1, e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Error loading PDF for OCR: {}", e.getMessage(), e);
            return "";
        }

        String result = fullText.toString().trim();
        logger.info("Total OCR text extracted: {} characters from {} pages", result.length(), pdfBytes.length);
        return result;
    }

    
    private String extractLargeFileWithBatchProcessing(byte[] pdfBytes) {
        StringBuilder fullText = new StringBuilder();

        if (tessDataPath == null) {
            logger.warn("OCR skipped — Tesseract not initialized");
            return "";
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            
            logger.info("Batch processing {} pages in groups of {}", pageCount, MAX_PAGES_PER_BATCH);

            for (int batchStart = 0; batchStart < pageCount; batchStart += MAX_PAGES_PER_BATCH) {
                int batchEnd = Math.min(batchStart + MAX_PAGES_PER_BATCH, pageCount);
                
                logger.info("Processing batch: pages {}-{} of {}", batchStart + 1, batchEnd, pageCount);

                for (int page = batchStart; page < batchEnd; page++) {
                    try {
                        // Use lower DPI for large files to speed up processing
                        int dpi = (pageCount > 50) ? 150 : 200;
                        BufferedImage image = renderer.renderImageWithDPI(page, dpi);
                        
                        BufferedImage optimizedImage = optimizeImageForOcr(image);
                        String ocrResult = performOcr(optimizedImage);

                        if (ocrResult != null && !ocrResult.trim().isEmpty()) {
                            fullText.append(ocrResult).append(" ");
                        }
                        
                        // Memory cleanup
                        image.flush();
                        optimizedImage.flush();

                    } catch (Exception e) {
                        logger.warn("Error processing page {}: {}", page + 1, e.getMessage());
                    }
                }
                
                // Force garbage collection between batches for large files
                if (pageCount > MAX_PAGES_PER_BATCH) {
                    System.gc();
                }
            }

        } catch (Exception e) {
            logger.error("Error in batch OCR processing: {}", e.getMessage(), e);
            return "";
        }

        String result = fullText.toString().trim();
        logger.info("Batch OCR complete: {} characters extracted", result.length());
        return result;
    }

    
    //  Optimize image for better OCR accuracy
    //
    private BufferedImage optimizeImageForOcr(BufferedImage original) {
        // Check if image needs resizing
        if (original.getWidth() > MAX_IMAGE_DIMENSION || original.getHeight() > MAX_IMAGE_DIMENSION) {
            return resizeImage(original, MAX_IMAGE_DIMENSION);
        }
        
        // Apply basic image processing for better OCR
        return applyImageProcessing(original);
    }

    
    private BufferedImage resizeImage(BufferedImage original, int maxDimension) {
        int width = original.getWidth();
        int height = original.getHeight();
        
        double scale = Math.min((double) maxDimension / width, (double) maxDimension / height);
        
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);
        
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();
        
        return resized;
    }

    
    private BufferedImage applyImageProcessing(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        
        BufferedImage processed = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = processed.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();
        
        return processed;
    }

    private String performOcr(BufferedImage image) {
        try {
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            logger.error("OCR failed: {}", e.getMessage());
            return "";
        } catch (Exception e) {
            logger.error("Unexpected error during OCR: {}", e.getMessage());
            return "";
        }
    }

   
    public boolean isInitialized() {
        return tessDataPath != null;
    }

   
    public String getStatus() {
        if (tessDataPath == null) {
            return "OCR Service: Not initialized (tessdata not found)";
        }
        return "OCR Service: Initialized (tessdata: " + tessDataPath + ")";
    }
}

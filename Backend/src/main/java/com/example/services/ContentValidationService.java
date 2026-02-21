package com.example.services;

import com.example.dto.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;


@Service
public class ContentValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ContentValidationService.class);

    @Autowired
    private OcrService ocrService;

    @Autowired
    private KeywordMatcher keywordMatcher;

    @Autowired
    private DuplicateDetector duplicateDetector;

   
    public ValidationResult validateDocumentContent(List<String> requiredDocuments, MultipartFile[] files) {
        logger.info("Starting CONTENT-BASED validation - Required: {}", requiredDocuments);

        ValidationResult result = new ValidationResult();

        if (requiredDocuments == null || requiredDocuments.isEmpty()) {
            result.setValid(true);
            result.setMessage("No required documents specified.");
            return result;
        }

        if (files == null || files.length == 0) {
            result.setValid(false);
            result.setMissingDocuments(new ArrayList<>(requiredDocuments));
            result.setMessage("No documents uploaded.");
            return result;
        }

        Map<String, String> extractedContent = extractTextFromFiles(files);

        // Validate each required document
        for (String requiredDoc : requiredDocuments) {
            boolean found = findDocumentInContent(requiredDoc, extractedContent, result);
            
            if (!found) {
                result.getMissingDocuments().add(requiredDoc);
            }
        }

        setValidationMessage(result);

        return result;
    }

    
    public ValidationResult validateWithRules(List<String> requiredDocuments, MultipartFile[] files) {
        logger.info("=== Starting RULE-BASED validation with OCR ===");
        logger.info("Required documents: {}", requiredDocuments);

        ValidationResult result = new ValidationResult();

        if (requiredDocuments == null || requiredDocuments.isEmpty()) {
            result.setValid(true);
            result.setMessage("No required documents specified.");
            return result;
        }

        if (files == null || files.length == 0) {
            result.setValid(false);
            result.setMissingDocuments(new ArrayList<>(requiredDocuments));
            result.setMessage("No documents uploaded.");
            return result;
        }

        // Step 0: Detect duplicate documents
        checkForDuplicates(files, result);

        // Step 1: Extract text from ALL uploaded files using OCR
        Map<String, String> extractedContents = extractTextFromFilesWithValidation(files);

        logger.info("Step 2: Validating {} required documents against extracted content", requiredDocuments.size());

        // Step 2: For each required document, search in ALL extracted PDF contents
        for (String requiredDoc : requiredDocuments) {
            boolean found = findDocumentWithRules(requiredDoc, extractedContents, result);
            
            if (!found) {
                result.getMissingDocuments().add(requiredDoc);
                logger.warn("✗✗ Document '{}' NOT FOUND in any PDF content", requiredDoc);
            }
        }

        // Add warnings for unvalidated files
        addWarningsForUnvalidatedFiles(extractedContents, result);

        // Set final validation message
        setValidationMessageWithDetails(result, requiredDocuments.size());

        return result;
    }

   
    private Map<String, String> extractTextFromFilesWithValidation(MultipartFile[] files) {
        Map<String, String> extractedContents = new HashMap<>();

        logger.info("Step 1: Processing {} uploaded files (PDFs and images)", files.length);

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String fileName = file.getOriginalFilename();

                // Validate file type
                String contentType = file.getContentType();
                if (contentType != null && !contentType.equals("application/pdf")) {
                    logger.warn("Skipping non-PDF file: {} (content-type: {})", fileName, contentType);
                    extractedContents.put(fileName != null ? fileName : "unknown",
                        "ERROR: Only PDF files are allowed");
                    continue;
                }

                if (fileName != null && !fileName.toLowerCase().endsWith(".pdf")) {
                    logger.warn("Skipping non-PDF file: {} (extension check)", fileName);
                    extractedContents.put(fileName != null ? fileName : "unknown",
                        "ERROR: Only PDF files are allowed");
                    continue;
                }

                // Extract text using OCR
                String content = extractTextFromPdf(file, fileName);
                extractedContents.put(fileName != null ? fileName : "unknown", content);
            }
        }

        return extractedContents;
    }

   
    private String extractTextFromPdf(MultipartFile file, String fileName) {
        try {
            logger.info("Extracting text from PDF file: {} using OCR...", fileName);
            String content = ocrService.extractText(file);

            boolean extractionFailed = content == null ||
                                     content.startsWith("Error") ||
                                     content.trim().isEmpty();

            if (extractionFailed) {
                logger.warn("Could not extract text from {} - Content: {}",
                    fileName, content != null && content.length() > 100 ? content.substring(0, 100) : content);
                return "IMAGE_PDF_FALLBACK:" + (fileName != null ? fileName : "");
            } else {
                logger.info("✓ Extracted {} characters from {} (OCR text available)", content.length(), fileName);
                return content;
            }
        } catch (Exception e) {
            logger.error("Exception extracting text from {}: {}", fileName, e.getMessage(), e);
            return "IMAGE_PDF_FALLBACK:" + (fileName != null ? fileName : "");
        }
    }

    private Map<String, String> extractTextFromFiles(MultipartFile[] files) {
        Map<String, String> extractedContent = new HashMap<>();

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String fileName = file.getOriginalFilename();

                String contentType = file.getContentType();
                if (contentType != null && !contentType.equals("application/pdf")) {
                    logger.warn("Skipping non-PDF file: {} (content-type: {})", fileName, contentType);
                    extractedContent.put(fileName != null ? fileName : "unknown", "");
                    continue;
                }

                if (fileName != null && !fileName.toLowerCase().endsWith(".pdf")) {
                    logger.warn("Skipping non-PDF file: {} (extension check)", fileName);
                    extractedContent.put(fileName != null ? fileName : "unknown", "");
                    continue;
                }

                try {
                    String extractedText = ocrService.extractText(file);
                    extractedContent.put(fileName != null ? fileName : "unknown", extractedText);
                    logger.info("Extracted text from {}: {} chars", fileName, extractedText.length());
                } catch (Exception e) {
                    logger.warn("Could not extract text from {}: {}", fileName, e.getMessage());
                    extractedContent.put(fileName != null ? fileName : "unknown", "");
                }
            }
        }

        return extractedContent;
    }

   
    private boolean findDocumentInContent(String requiredDoc, Map<String, String> extractedContent, ValidationResult result) {
        String requiredDocLower = requiredDoc.toLowerCase().trim();
        boolean found = false;

        boolean isPanCard = requiredDocLower.contains("pan");
        String foundPanNumber = null;

        List<String> keywords = keywordMatcher.getKeywordsForDocument(requiredDocLower);
        if (!keywords.contains(requiredDocLower)) {
            keywords.add(requiredDocLower);
        }

        for (Map.Entry<String, String> entry : extractedContent.entrySet()) {
            String fileName = entry.getKey().toLowerCase();
            String content = entry.getValue().toLowerCase();

            if (isPanCard) {
                foundPanNumber = keywordMatcher.validatePanCardInContent(entry.getValue());
                if (foundPanNumber != null) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [PAN: " + foundPanNumber + "] -> " + entry.getKey());
                    logger.info("PAN card validated via PAN number format '{}' in {}", foundPanNumber, entry.getKey());
                    break;
                }
            }

            if (!found && keywordMatcher.checkContentMatch(requiredDocLower, keywords, fileName, content)) {
                found = true;
                result.getMatchedDocuments().add(requiredDoc + " [CONTENT] -> " + entry.getKey());
                logger.info("Content matched: '{}' in {}", requiredDoc, entry.getKey());
                break;
            }
        }

        return found;
    }

    
    private boolean findDocumentWithRules(String requiredDoc, Map<String, String> extractedContents, ValidationResult result) {
        String requiredDocLower = requiredDoc.toLowerCase().trim();
        boolean found = false;
        String foundInFile = "";

        List<String> keywords = keywordMatcher.getKeywordsForDocument(requiredDocLower);
        logger.info("Validating required document: '{}'", requiredDoc);
        logger.info("  Keywords to search: {}", keywords);

        boolean isPanCard = requiredDocLower.contains("pan");
        String foundPanNumber = null;

        boolean isAadharCard = requiredDocLower.contains("aadhar") || requiredDocLower.contains("aadhaar");
        String foundAadharNumber = null;

      
        for (Map.Entry<String, String> entry : extractedContents.entrySet()) {
            String fileName = entry.getKey();
            String content = entry.getValue();

            logger.debug("  Checking file: {}", fileName);

            if (content.startsWith("IMAGE_PDF_FALLBACK")) {
                logger.warn("  File {} has no extractable text (OCR failed). Cannot validate using filename - filename matching is not allowed.", fileName);
                continue;
            }

            String contentLower = content.toLowerCase().trim();

            if (contentLower.startsWith("error") || contentLower.isEmpty()) {
                logger.warn("  Skipping {} - no valid content extracted. Content: '{}'", fileName,
                    content.length() > 100 ? content.substring(0, 100) : content);
                continue;
            }

            logger.info("  Content length: {} characters", content.length());

            // Special validation for PAN card
            if (isPanCard) {
                String panResult = keywordMatcher.validatePanCardInContent(content);
                if (panResult != null) {
                    found = true;
                    foundInFile = fileName;
                    foundPanNumber = panResult;
                    logger.info("  ✓ PAN card validated in {}", fileName);
                }
            }

            // Special validation for Aadhaar card
            if (!found && isAadharCard) {
                String aadharResult = keywordMatcher.validateAadharCardInContent(content);
                if (aadharResult != null) {
                    found = true;
                    foundInFile = fileName;
                    foundAadharNumber = aadharResult;
                    logger.info("  ✓ Aadhaar card validated in {}", fileName);
                }
            }

            // If not found with specific validation, check keywords in OCR text
            if (!found) {
                int matchCount = searchKeywordsInContent(requiredDocLower, keywords, contentLower);

                if (matchCount > 0) {
                    found = true;
                    foundInFile = fileName;
                    logger.info("  ✓✓ FOUND '{}' - matched {} keyword(s) in PDF content of {}",
                        requiredDoc, matchCount, fileName);
                }
            }

            if (found) break;
        }

        // Record result
        if (found) {
            String matchDetail = buildMatchDetail(requiredDoc, foundInFile, isPanCard, foundPanNumber,
                isAadharCard, foundAadharNumber);
            result.getMatchedDocuments().add(matchDetail);
            logger.info("✓✓ Document '{}' VALIDATED via OCR content", requiredDoc);
            return true;
        } else {
            logger.warn("✗✗ Document '{}' NOT FOUND in any PDF content", requiredDoc);
            logger.warn("  Searched keywords: {}", keywords);
            return false;
        }
    }

    /**
     * Search for keywords in content
     */
    private int searchKeywordsInContent(String requiredDocLower, List<String> keywords, String contentLower) {
        int matchCount = 0;

        for (String kw : keywords) {
            String kwLower = kw.toLowerCase().trim();
            if (kwLower.isEmpty()) continue;

            boolean keywordFound = contentLower.contains(kwLower);

            if (keywordFound) {
                matchCount++;
                logger.info("  ✓ Found keyword '{}' in content", kw);
            } else {
                logger.debug("  ✗ Keyword '{}' not found in content", kw);
            }
        }

        if (matchCount == 0 && !requiredDocLower.isEmpty()) {
            if (contentLower.contains(requiredDocLower)) {
                matchCount++;
                logger.info("  ✓ Found required document name '{}' directly in content", requiredDocLower);
            }
        }

        return matchCount;
    }

    /**
     * Build match detail string
     */
    private String buildMatchDetail(String requiredDoc, String foundInFile, boolean isPanCard,
        String foundPanNumber, boolean isAadharCard, String foundAadharNumber) {

        String matchDetail = requiredDoc + " -> " + foundInFile;
        if (isPanCard && foundPanNumber != null) {
            if (!foundPanNumber.equals("PAN_KEYWORD_FOUND")) {
                matchDetail += " (PAN: " + foundPanNumber + ")";
            } else {
                matchDetail += " (PAN keywords detected)";
            }
        } else if (isAadharCard && foundAadharNumber != null) {
            if (!foundAadharNumber.equals("AADHAR_KEYWORD_FOUND")) {
                matchDetail += " (Aadhaar: " + foundAadharNumber + ")";
            } else {
                matchDetail += " (Aadhaar keywords detected)";
            }
        } else {
            matchDetail += " (OCR content matched)";
        }

        return matchDetail;
    }

    /**
     * Check for duplicate documents
     */
    private void checkForDuplicates(MultipartFile[] files, ValidationResult result) {
        logger.info("Step 0: Checking for duplicate documents...");
        List<String> duplicates = duplicateDetector.getDuplicateFileNames(files);
        if (!duplicates.isEmpty()) {
            result.getDuplicateDocuments().addAll(duplicates);
            logger.warn("Found {} duplicate documents: {}", duplicates.size(), duplicates);
        }
    }

    /**
     * Add warnings for files that couldn't be validated
     */
    private void addWarningsForUnvalidatedFiles(Map<String, String> extractedContents, ValidationResult result) {
        List<String> unvalidatedFiles = new ArrayList<>();
        for (Map.Entry<String, String> entry : extractedContents.entrySet()) {
            if (entry.getValue().startsWith("IMAGE_PDF_FALLBACK")) {
                unvalidatedFiles.add(entry.getKey() + " (OCR failed - content not extractable)");
            }
        }
        if (!unvalidatedFiles.isEmpty()) {
            result.getWarnings().add("Warning: The following files could not be validated because text extraction failed: " + String.join(", ", unvalidatedFiles));
            logger.warn("=== WARNING: {} files could not be validated due to OCR failure ===", unvalidatedFiles.size());
        }
    }

    /**
     * Set validation message (simple version)
     */
    private void setValidationMessage(ValidationResult result) {
        if (result.getMissingDocuments().isEmpty()) {
            result.setValid(true);
            result.setMessage("All required documents validated via content!");
        } else {
            result.setValid(false);
            result.setMessage("Missing: " + String.join(", ", result.getMissingDocuments()));
        }
    }

    /**
     * Set validation message with details
     */
    private void setValidationMessageWithDetails(ValidationResult result, int totalRequired) {
        if (result.getMissingDocuments().isEmpty()) {
            result.setValid(true);
            result.setMessage("All documents validated via OCR content!");
            logger.info("=== VALIDATION SUCCESS: All {} documents validated ===", totalRequired);
        } else {
            result.setValid(false);
            result.setMessage("Missing documents (not found in OCR content): " + String.join(", ", result.getMissingDocuments()));
            logger.warn("=== VALIDATION FAILED: {} of {} documents missing ===",
                result.getMissingDocuments().size(), totalRequired);
        }
    }
}

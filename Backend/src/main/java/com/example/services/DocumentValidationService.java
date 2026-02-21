package com.example.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;


@Service
public class DocumentValidationService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentValidationService.class);
    
    @Autowired
    private OcrService ocrService;

    private static final Map<String, List<String>> DOCUMENT_KEYWORDS = new HashMap<>();

    private static final Pattern PAN_CARD_PATTERN = Pattern.compile(
        "\\b[A-Z]{5}[0-9]{4}[A-Z]\\b"
    );
    
    private static final Pattern AADHAR_CARD_PATTERN = Pattern.compile(
        "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"
    );

    private static final List<String> PAN_KEYWORDS = Arrays.asList(
        "pan", "permanent account number", "permanent account", 
        "income tax", "pan card", "pan number", "pannumber", "pan card number",
        "income tax department"
    );
    
    // Keywords for Aadhaar card detection
    private static final List<String> AADHAR_KEYWORDS = Arrays.asList(
        "aadhar", "aadhaar", "uidai", "uid", "aadhar card", "aadhaar card",
        "unique identification", "uid number", "uidai letter"
    );
    
    static {
        DOCUMENT_KEYWORDS.put("AADHAR", Arrays.asList("aadhar", "aadhaar", "uidai", "uid", "aadhar card", "aadhaar card", "unique identification", "uid number"));
        DOCUMENT_KEYWORDS.put("PAN", Arrays.asList("pan", "permanent account", "income tax", "pan card", "pan number"));
        DOCUMENT_KEYWORDS.put("AADHAAR", Arrays.asList("aadhar", "aadhaar", "uidai", "uid"));
        DOCUMENT_KEYWORDS.put("GST", Arrays.asList("gst", "goods and services", "gstin", "tax registration"));
        DOCUMENT_KEYWORDS.put("TENDER", Arrays.asList("tender", "bid document", "proposal"));
        DOCUMENT_KEYWORDS.put("COMPANY_REGISTRATION", Arrays.asList("company registration", "moa", "aoa", "incorporation"));
        DOCUMENT_KEYWORDS.put("POWER_OF_ATTORNEY", Arrays.asList("power of attorney", "poa", "authorization"));
        DOCUMENT_KEYWORDS.put("BANK_STATEMENT", Arrays.asList("bank statement", "bank account"));
        DOCUMENT_KEYWORDS.put("AUDITED_FINANCIALS", Arrays.asList("audited", "financial statements", "balance sheet"));
        DOCUMENT_KEYWORDS.put("WORK_EXPERIENCE", Arrays.asList("work experience", "completed projects"));
        DOCUMENT_KEYWORDS.put("TECHNICAL_CAPABILITY", Arrays.asList("technical", "capability statement"));
        DOCUMENT_KEYWORDS.put("QUALITY_CERTIFICATE", Arrays.asList("quality", "iso", "certification"));
        DOCUMENT_KEYWORDS.put("EMD", Arrays.asList("emd", "earnest money", "security deposit", "bid security"));
        DOCUMENT_KEYWORDS.put("TENDER_FEE", Arrays.asList("tender fee", "document fee"));
        DOCUMENT_KEYWORDS.put("AFFIDAVIT", Arrays.asList("affidavit", "declaration", "undertaking"));
        DOCUMENT_KEYWORDS.put("NATIONALITY", Arrays.asList("nationality", "citizenship"));
        DOCUMENT_KEYWORDS.put("REGISTRATION", Arrays.asList("registration", "license", "permit"));
    }

    public static class ValidationResult {
        private boolean valid;
        private List<String> matchedDocuments = new ArrayList<>();
        private List<String> missingDocuments = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> duplicateDocuments = new ArrayList<>();
        private String message;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public List<String> getMatchedDocuments() { return matchedDocuments; }
        public void setMatchedDocuments(List<String> matchedDocuments) { this.matchedDocuments = matchedDocuments; }
        public List<String> getMissingDocuments() { return missingDocuments; }
        public void setMissingDocuments(List<String> missingDocuments) { this.missingDocuments = missingDocuments; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
        public List<String> getDuplicateDocuments() { return duplicateDocuments; }
        public void setDuplicateDocuments(List<String> duplicateDocuments) { this.duplicateDocuments = duplicateDocuments; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    public Map<String, List<String>> detectDuplicates(MultipartFile[] files) {
        Map<String, List<String>> hashToFiles = new HashMap<>();
        
        if (files == null || files.length == 0) {
            return hashToFiles;
        }
        
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                try {
                    String hash = calculateFileHash(file);
                    String fileName = file.getOriginalFilename();
                    
                    hashToFiles.computeIfAbsent(hash, k -> new ArrayList<>()).add(fileName);
                    logger.info("File '{}' hash: {}", fileName, hash);
                } catch (Exception e) {
                    logger.warn("Could not calculate hash for file: {}", file.getOriginalFilename());
                }
            }
        }
        
        return hashToFiles;
    }
    private String calculateFileHash(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = file.getBytes();
            byte[] hashBytes = md.digest(fileBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.warn("Could not calculate hash: {}", e.getMessage());
            return "hash_error_" + System.currentTimeMillis();
        }
    }
    
    public List<String> getDuplicateFileNames(MultipartFile[] files) {
        List<String> duplicates = new ArrayList<>();
        Map<String, List<String>> hashToFiles = detectDuplicates(files);
        
        for (Map.Entry<String, List<String>> entry : hashToFiles.entrySet()) {
            if (entry.getValue().size() > 1) {
                // Keep the first file, mark rest as duplicates
                for (int i = 1; i < entry.getValue().size(); i++) {
                    duplicates.add(entry.getValue().get(i) + " (duplicate of " + entry.getValue().get(0) + ")");
                }
            }
        }
        
        return duplicates;
    }
    
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
                
                // Also check file extension
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
        
        // Validate each required document
        for (String requiredDoc : requiredDocuments) {
            String requiredDocLower = requiredDoc.toLowerCase().trim();
            boolean found = false;
            
            // Special handling for PAN card
            boolean isPanCard = requiredDocLower.contains("pan");
            String foundPanNumber = null;
            
            List<String> keywords = getKeywordsInternal(requiredDocLower);
            if (!keywords.contains(requiredDocLower)) {
                keywords.add(requiredDocLower);
            }
            
            for (Map.Entry<String, String> entry : extractedContent.entrySet()) {
                String fileName = entry.getKey().toLowerCase();
                String content = entry.getValue().toLowerCase();
                
                // Special validation for PAN card
                if (isPanCard) {
                    foundPanNumber = validatePanCardInContent(entry.getValue());
                    if (foundPanNumber != null) {
                        found = true;
                        result.getMatchedDocuments().add(requiredDoc + " [PAN: " + foundPanNumber + "] -> " + entry.getKey());
                        logger.info("PAN card validated via PAN number format '{}' in {}", foundPanNumber, entry.getKey());
                        break;
                    }
                }
                
                if (!found && checkContentMatch(requiredDocLower, keywords, fileName, content)) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [CONTENT] -> " + entry.getKey());
                    logger.info("Content matched: '{}' in {}", requiredDoc, entry.getKey());
                    break;
                }
            }
            
            if (!found) {
                result.getMissingDocuments().add(requiredDoc);
            }
        }
        
        if (result.getMissingDocuments().isEmpty()) {
            result.setValid(true);
            result.setMessage("All required documents validated via content!");
        } else {
            result.setValid(false);
            result.setMessage("Missing: " + String.join(", ", result.getMissingDocuments()));
        }
        
        return result;
    }
    
    // ========== RULE-BASED VALIDATION ==========
    // Validates based on actual PDF CONTENT using OCR and keyword matching
    
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
        
        // Step 0: Detect duplicate documents using basic hashing
        logger.info("Step 0: Checking for duplicate documents...");
        List<String> duplicates = getDuplicateFileNames(files);
        if (!duplicates.isEmpty()) {
            result.getDuplicateDocuments().addAll(duplicates);
            logger.warn("Found {} duplicate documents: {}", duplicates.size(), duplicates);
        }
        
        // Step 1: Extract text from ALL uploaded files using OCR
        // Convert images to PDF if needed
        logger.info("Step 1: Processing {} uploaded files (PDFs and images)", files.length);
        Map<String, String> extractedContents = new HashMap<>();
        
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String fileName = file.getOriginalFilename();
                
                // Validate PDF file type (should already be validated in controller, but double-check)
                String contentType = file.getContentType();
                if (contentType != null && !contentType.equals("application/pdf")) {
                    logger.warn("Skipping non-PDF file: {} (content-type: {})", fileName, contentType);
                    extractedContents.put(fileName != null ? fileName : "unknown", 
                        "ERROR: Only PDF files are allowed");
                    continue;
                }
                
                // Also check file extension
                if (fileName != null && !fileName.toLowerCase().endsWith(".pdf")) {
                    logger.warn("Skipping non-PDF file: {} (extension check)", fileName);
                    extractedContents.put(fileName != null ? fileName : "unknown", 
                        "ERROR: Only PDF files are allowed");
                    continue;
                }
                
                try {
                    // Extract text using OCR service (handles both text PDFs and image-based PDFs)
                    logger.info("Extracting text from PDF file: {} using OCR...", fileName);
                    String content = ocrService.extractText(file);
                    
                    // Check if content extraction failed
                    boolean extractionFailed = content == null || 
                                             content.startsWith("Error") || 
                                             content.trim().isEmpty();
                    
                    if (extractionFailed) {
                        logger.warn("Could not extract text from {} - Content: {}", 
                            fileName, content != null && content.length() > 100 ? content.substring(0, 100) : content);
                        // Store as fallback for filename-based matching
                        extractedContents.put(fileName != null ? fileName : "unknown", 
                            "IMAGE_PDF_FALLBACK:" + (fileName != null ? fileName : ""));
                    } else {
                        // Use the extracted content (OCR result)
                        extractedContents.put(fileName != null ? fileName : "unknown", content);
                        logger.info("✓ Extracted {} characters from {} (OCR text available)", content.length(), fileName);
                        // Log sample of extracted text for debugging
                        if (content.length() > 0) {
                            String sample = content.length() > 200 ? content.substring(0, 200) + "..." : content;
                            logger.debug("Sample extracted text from {}: {}", fileName, sample);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Exception extracting text from {}: {}", fileName, e.getMessage(), e);
                    extractedContents.put(fileName != null ? fileName : "unknown", 
                        "IMAGE_PDF_FALLBACK:" + (fileName != null ? fileName : ""));
                }
            }
        }
        
        logger.info("Step 2: Validating {} required documents against extracted content", requiredDocuments.size());
        
        // Step 2: For each required document, search in ALL extracted PDF contents
        for (String requiredDoc : requiredDocuments) {
            String requiredDocLower = requiredDoc.toLowerCase().trim();
            boolean found = false;
            String foundInFile = "";
            
            // Get comprehensive keywords for this document type
            List<String> keywords = getKeywordsForDocument(requiredDocLower);
            logger.info("Validating required document: '{}'", requiredDoc);
            logger.info("  Keywords to search: {}", keywords);
            
            // Special handling for PAN card - check for PAN number format
            boolean isPanCard = requiredDocLower.contains("pan");
            String foundPanNumber = null;
            
            // Special handling for Aadhaar card - check for Aadhaar number format
            boolean isAadharCard = requiredDocLower.contains("aadhar") || requiredDocLower.contains("aadhaar");
            String foundAadharNumber = null;
            
            // Search in each PDF's content
            for (Map.Entry<String, String> entry : extractedContents.entrySet()) {
                String fileName = entry.getKey();
                String content = entry.getValue();
                
                logger.debug("  Checking file: {}", fileName);
                
                // Check if this is an image-based PDF with no OCR text
                // File names shall NOT be used for validation per requirements
                // Instead, add a warning that this document could not be validated
                if (content.startsWith("IMAGE_PDF_FALLBACK")) {
                    logger.warn("  File {} has no extractable text (OCR failed). Cannot validate using filename - filename matching is not allowed.", fileName);
                    // Do NOT use filename for validation - add warning instead
                    // The document will be marked as potentially missing
                    // Continue to check other files
                    continue;
                }
                
                // For PDFs with OCR text, check content
                String contentLower = content.toLowerCase().trim();
                
                // Skip if content extraction failed
                if (contentLower.startsWith("error") || contentLower.isEmpty()) {
                    logger.warn("  Skipping {} - no valid content extracted. Content: '{}'", fileName, 
                        content.length() > 100 ? content.substring(0, 100) : content);
                    continue;
                }
                
                logger.info("  Content length: {} characters", content.length());
                logger.info("  Content preview (first 300 chars): {}", 
                    content.length() > 300 ? content.substring(0, 300) + "..." : content);
                
                // Special validation for PAN card
                if (isPanCard) {
                    String panResult = validatePanCardInContent(content);
                    if (panResult != null) {
                        found = true;
                        foundInFile = fileName;
                        foundPanNumber = panResult;
                        if (panResult.equals("PAN_KEYWORD_FOUND")) {
                            logger.info("  ✓ PAN card validated via PAN keywords in {}", fileName);
                        } else {
                            logger.info("  ✓ PAN card validated via PAN number format '{}' in {}", panResult, fileName);
                        }
                    }
                }
                
                // Special validation for Aadhaar card
                if (!found && isAadharCard) {
                    String aadharResult = validateAadharCardInContent(content);
                    if (aadharResult != null) {
                        found = true;
                        foundInFile = fileName;
                        foundAadharNumber = aadharResult;
                        if (aadharResult.equals("AADHAR_KEYWORD_FOUND")) {
                            logger.info("  ✓ Aadhaar card validated via Aadhaar keywords in {}", fileName);
                        } else {
                            logger.info("  ✓ Aadhaar card validated via Aadhaar number format '{}' in {}", aadharResult, fileName);
                        }
                    }
                }
                
                // If not found with PAN-specific validation, check keywords in OCR text
                if (!found) {
                    int matchCount = 0;
                    String matchedKeyword = null;
                    
                    // Log what we're searching for
                    logger.info("  Searching for keywords in content: {}", keywords);
                    logger.info("  Content sample (first 500 chars): {}", 
                        content.length() > 500 ? content.substring(0, 500) : content);
                    
                    // Check if any keyword is found in the PDF content (case-insensitive)
                    for (String kw : keywords) {
                        String kwLower = kw.toLowerCase().trim();
                        if (kwLower.isEmpty()) continue;
                        
                        // Check if keyword exists in content (case-insensitive)
                        boolean keywordFound = contentLower.contains(kwLower);
                        
                        if (keywordFound) {
                            matchCount++;
                            if (matchedKeyword == null) {
                                matchedKeyword = kw;
                            }
                            logger.info("  ✓ Found keyword '{}' in content", kw);
                        } else {
                            logger.debug("  ✗ Keyword '{}' not found in content", kw);
                        }
                    }
                    
                    // Also check if the required document name itself is in the content
                    if (matchCount == 0 && !requiredDocLower.isEmpty()) {
                        if (contentLower.contains(requiredDocLower)) {
                            matchCount++;
                            matchedKeyword = requiredDoc;
                            logger.info("  ✓ Found required document name '{}' directly in content", requiredDoc);
                        }
                    }
                    
                    // Match if at least one keyword found (flexible matching)
                    if (matchCount > 0) {
                        found = true;
                        foundInFile = fileName;
                        logger.info("  ✓✓ FOUND '{}' - matched {} keyword(s) including '{}' in PDF content of {}", 
                            requiredDoc, matchCount, matchedKeyword, fileName);
                    } else {
                        logger.warn("  ✗✗ No keywords found for '{}' in file {}", requiredDoc, fileName);
                        logger.warn("  Searched keywords were: {}", keywords);
                    }
                }
                
                if (found) break;
            }
            
            // Record result
            if (found) {
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
                result.getMatchedDocuments().add(matchDetail);
                logger.info("✓✓ Document '{}' VALIDATED via OCR content", requiredDoc);
            } else {
                result.getMissingDocuments().add(requiredDoc);
                logger.warn("✗✗ Document '{}' NOT FOUND in any PDF content", requiredDoc);
                logger.warn("  Searched keywords: {}", keywords);
            }
        }
        
        // Final result
        if (result.getMissingDocuments().isEmpty()) {
            result.setValid(true);
            result.setMessage("All documents validated via OCR content!");
            logger.info("=== VALIDATION SUCCESS: All {} documents validated ===", requiredDocuments.size());
        } else {
            result.setValid(false);
            result.setMessage("Missing documents (not found in OCR content): " + String.join(", ", result.getMissingDocuments()));
            logger.warn("=== VALIDATION FAILED: {} of {} documents missing ===", 
                result.getMissingDocuments().size(), requiredDocuments.size());
        }
        
        // Add warning if some files couldn't be validated (OCR failed)
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
        
        return result;
    }
    
    private boolean checkContentMatch(String requiredDoc, List<String> keywords, String fileName, String content) {
        if (matchesRequiredDocument(requiredDoc, fileName)) return true;
        if (content == null || content.isEmpty()) return false;
        
        int matches = 0;
        for (String keyword : keywords) {
            if (content.contains(keyword.toLowerCase())) matches++;
        }
        
        return matches >= 2;
    }
    
    private boolean matchesRequiredDocument(String requiredDoc, String fileName) {
        if (fileName.contains(requiredDoc)) return true;
        
        for (Map.Entry<String, List<String>> entry : DOCUMENT_KEYWORDS.entrySet()) {
            String docType = entry.getKey().toLowerCase();
            List<String> keywords = entry.getValue();
            
            if (requiredDoc.contains(docType) || docType.contains(requiredDoc)) {
                for (String kw : keywords) {
                    if (fileName.contains(kw)) return true;
                }
            }
            
            for (String kw : keywords) {
                if (requiredDoc.contains(kw) && fileName.contains(kw)) return true;
            }
        }
        
        String[] words = requiredDoc.split("[\\s,_-]+");
        for (String word : words) {
            if (word.length() > 3 && fileName.contains(word)) return true;
        }
        
        return false;
    }
    
    private List<String> getKeywordsInternal(String documentType) {
        String docTypeLower = documentType.toLowerCase().trim();
        
        // First try exact match (case-insensitive)
        for (Map.Entry<String, List<String>> entry : DOCUMENT_KEYWORDS.entrySet()) {
            if (entry.getKey().toLowerCase().equals(docTypeLower)) {
                logger.debug("Found exact match for '{}' in DOCUMENT_KEYWORDS", documentType);
                return entry.getValue();
            }
        }
        
        // Try partial matching - check if documentType contains any key or vice versa
        for (Map.Entry<String, List<String>> entry : DOCUMENT_KEYWORDS.entrySet()) {
            String key = entry.getKey().toLowerCase();
            
            // Check if document type contains the key or key contains document type
            // Also check for common variations like "pan card" vs "PAN"
            if (docTypeLower.contains(key) || key.contains(docTypeLower) ||
                (key.equals("pan") && (docTypeLower.contains("pan") || docTypeLower.contains("pan card"))) ||
                (key.equals("aadhar") && (docTypeLower.contains("aadhar") || docTypeLower.contains("aadhaar"))) ||
                (key.equals("gst") && docTypeLower.contains("gst"))) {
                logger.debug("Found partial match: '{}' matches key '{}'", documentType, entry.getKey());
                return entry.getValue();
            }
        }
        
        // Special case: check for common document names
        if (docTypeLower.contains("pan") || docTypeLower.contains("income tax")) {
            logger.debug("Special case: '{}' matched to PAN keywords", documentType);
            return DOCUMENT_KEYWORDS.get("PAN");
        }
        
        if (docTypeLower.contains("aadhar") || docTypeLower.contains("aadhaar") || docTypeLower.contains("uid")) {
            logger.debug("Special case: '{}' matched to AADHAR keywords", documentType);
            return DOCUMENT_KEYWORDS.get("AADHAR");
        }
        
        if (docTypeLower.contains("gst")) {
            logger.debug("Special case: '{}' matched to GST keywords", documentType);
            return DOCUMENT_KEYWORDS.get("GST");
        }
        
        logger.debug("No keywords found for '{}'", documentType);
        return new ArrayList<>();
    }
    
    /**
     * Get keywords for a document - combines predefined keywords with words from the document name
     * This ensures comprehensive keyword matching for OCR validation
     */
    private List<String> getKeywordsForDocument(String documentName) {
        List<String> keywords = new ArrayList<>();
        String docNameLower = documentName.toLowerCase().trim();
        
        // First try to get predefined keywords from DOCUMENT_KEYWORDS map
        List<String> predefined = getKeywordsInternal(docNameLower);
        if (predefined != null && !predefined.isEmpty()) {
            keywords.addAll(predefined);
            logger.debug("Added {} predefined keywords for '{}': {}", predefined.size(), documentName, predefined);
        }
        
        // ALWAYS add the full document name as a keyword (critical for matching)
        if (!docNameLower.isEmpty() && !keywords.contains(docNameLower)) {
            keywords.add(docNameLower);
        }
        
        // Add individual words from the document name (for flexible matching)
        String[] words = docNameLower.split("[\\s,_-]+");
        for (String word : words) {
            word = word.trim();
            // Add words that are meaningful (length >= 2 and not already added)
            if (word.length() >= 2 && !keywords.contains(word)) {
                keywords.add(word);
            }
        }
        
        // Also check if document name contains any predefined document type keywords
        // For example, if requiredDoc is "Pan Card", check for "PAN" keywords
        for (Map.Entry<String, List<String>> entry : DOCUMENT_KEYWORDS.entrySet()) {
            String docType = entry.getKey().toLowerCase();
            if (docNameLower.contains(docType) || docType.contains(docNameLower)) {
                List<String> typeKeywords = entry.getValue();
                for (String typeKw : typeKeywords) {
                    if (!keywords.contains(typeKw.toLowerCase())) {
                        keywords.add(typeKw.toLowerCase());
                    }
                }
            }
        }
        
        // Remove duplicates while preserving order
        List<String> uniqueKeywords = new ArrayList<>();
        for (String kw : keywords) {
            String kwLower = kw.toLowerCase().trim();
            if (!kwLower.isEmpty() && !uniqueKeywords.contains(kwLower)) {
                uniqueKeywords.add(kwLower);
            }
        }
        
        logger.info("Final keywords for '{}': {}", documentName, uniqueKeywords);
        return uniqueKeywords;
    }
    
    /**
     * Validate PAN card in document content
     * Checks for:
     * 1. PAN card number pattern (ABCDE1234F format) - PRIMARY check
     * 2. PAN-related keywords - SECONDARY check
     * Returns the found PAN number if valid, null otherwise
     */
    private String validatePanCardInContent(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        
        // PRIMARY: Try to find PAN card number pattern first (most reliable)
        // PAN format: ABCDE1234F (5 letters, 4 numbers, 1 letter)
        Matcher matcher = PAN_CARD_PATTERN.matcher(content);
        if (matcher.find()) {
            String panNumber = matcher.group();
            logger.info("Found valid PAN card number: {}", panNumber);
            return panNumber;
        }
        
        // Also try case-insensitive PAN number patterns
        String[] panPatterns = {
            "pan\\s*[:|=]\\s*([A-Z]{5}[0-9]{4}[A-Z])",
            "panno\\s*[:|=]\\s*([A-Z]{5}[0-9]{4}[A-Z])",
            "pan\\s*number\\s*[:|=]\\s*([A-Z]{5}[0-9]{4}[A-Z])",
            "permanent\\s*account\\s*number\\s*[:|=]\\s*([A-Z]{5}[0-9]{4}[A-Z])"
        };
        
        for (String pattern : panPatterns) {
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(content);
            if (m.find()) {
                String panNumber = m.group(1);
                logger.info("Found PAN card number with pattern: {}", panNumber);
                return panNumber;
            }
        }
        
        // SECONDARY: If no PAN number found, check for PAN keywords
        // This is a fallback in case OCR missed the number but detected keywords
        String contentLower = content.toLowerCase();
        boolean hasPanKeyword = false;
        for (String keyword : PAN_KEYWORDS) {
            if (contentLower.contains(keyword)) {
                hasPanKeyword = true;
                logger.info("Found PAN keyword: {}", keyword);
                break;
            }
        }
        
        if (hasPanKeyword) {
            // Keywords found - try one more time with more flexible pattern
            // Look for any 10-char alphanumeric that could be PAN
            Pattern flexiblePan = Pattern.compile("\\b[A-Za-z]{5}[0-9]{4}[A-Za-z]\\b");
            Matcher flexMatcher = flexiblePan.matcher(content);
            if (flexMatcher.find()) {
                String panNumber = flexMatcher.group();
                logger.info("Found potential PAN card number (flexible): {}", panNumber);
                return panNumber.toUpperCase();
            }
            
            // If we found keywords but no number, still return success with a marker
            logger.info("PAN keywords found but no valid PAN number format detected");
            return "PAN_KEYWORD_FOUND";
        }
        
        logger.debug("No PAN keywords or PAN number found in content");
        return null;
    }
    
    /**
     * Validate Aadhaar card in document content
     * Checks for:
     * 1. Aadhaar number pattern (12 digits with optional spaces/dashes) - PRIMARY check
     * 2. Aadhaar-related keywords - SECONDARY check
     * Returns the found Aadhaar number if valid, null otherwise
     */
    private String validateAadharCardInContent(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        
        // PRIMARY: Try to find Aadhaar number pattern first (most reliable)
        // Aadhaar format: 12 digits (can have spaces or dashes)
        Matcher matcher = AADHAR_CARD_PATTERN.matcher(content);
        if (matcher.find()) {
            String aadharNumber = matcher.group();
            logger.info("Found valid Aadhaar card number: {}", aadharNumber);
            return aadharNumber;
        }
        
        // Also try without spaces/dashes
        String cleanContent = content.replaceAll("[\\s-]", "");
        if (cleanContent.length() >= 12) {
            // Try to find 12 consecutive digits
            Pattern twelveDigits = Pattern.compile("\\b\\d{12}\\b");
            Matcher m = twelveDigits.matcher(cleanContent);
            if (m.find()) {
                String aadharNumber = m.group();
                logger.info("Found Aadhaar card number (cleaned): {}", aadharNumber);
                return aadharNumber;
            }
        }
        
        // SECONDARY: If no Aadhaar number found, check for Aadhaar keywords
        String contentLower = content.toLowerCase();
        for (String keyword : AADHAR_KEYWORDS) {
            if (contentLower.contains(keyword)) {
                logger.info("Found Aadhaar keyword: {}", keyword);
                return "AADHAR_KEYWORD_FOUND";
            }
        }
        
        logger.debug("No Aadhaar keywords or Aadhaar number found in content");
        return null;
    }
    
    // ========== FILENAME-ONLY VALIDATION (Fallback) ==========
    
    /**
     * Validate documents using filename matching (fallback when OCR fails)
     */
    public ValidationResult validateDocuments(List<String> requiredDocuments, List<String> uploadedFileNames) {
        ValidationResult result = new ValidationResult();
        
        if (requiredDocuments == null || requiredDocuments.isEmpty()) {
            result.setValid(true);
            result.setMessage("No required documents specified.");
            return result;
        }
        
        if (uploadedFileNames == null || uploadedFileNames.isEmpty()) {
            result.setValid(false);
            result.setMissingDocuments(new ArrayList<>(requiredDocuments));
            result.setMessage("No documents uploaded.");
            return result;
        }
        
        for (String requiredDoc : requiredDocuments) {
            String requiredDocLower = requiredDoc.toLowerCase().trim();
            boolean found = false;
            
            for (String uploadedFile : uploadedFileNames) {
                if (matchesRequiredDocument(requiredDocLower, uploadedFile.toLowerCase())) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + uploadedFile);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                result.getMissingDocuments().add(requiredDoc);
            }
        }
        
        if (result.getMissingDocuments().isEmpty()) {
            result.setValid(true);
            result.setMessage("All documents validated!");
        } else {
            result.setValid(false);
            result.setMessage("Missing: " + String.join(", ", result.getMissingDocuments()));
        }
        
        return result;
    }
    
    /**
     * Validate documents using fuzzy filename matching (fallback)
     */
    public ValidationResult validateDocumentsFuzzy(List<String> requiredDocuments, List<String> uploadedFileNames) {
        ValidationResult result = new ValidationResult();
        
        if (requiredDocuments == null || requiredDocuments.isEmpty()) {
            result.setValid(true);
            result.setMessage("No required documents.");
            return result;
        }
        
        if (uploadedFileNames == null || uploadedFileNames.isEmpty()) {
            result.setValid(false);
            result.setMissingDocuments(new ArrayList<>(requiredDocuments));
            result.setMessage("No documents uploaded.");
            return result;
        }
        
        for (String requiredDoc : requiredDocuments) {
            String requiredDocLower = requiredDoc.toLowerCase().trim();
            boolean found = false;
            
            for (String uploadedFile : uploadedFileNames) {
                String fileNameLower = uploadedFile.toLowerCase().trim();
                double similarity = calculateSimilarity(requiredDocLower, fileNameLower);
                
                if (similarity >= 0.5) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + uploadedFile);
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                result.getMissingDocuments().add(requiredDoc);
            }
        }
        
        if (result.getMissingDocuments().isEmpty()) {
            result.setValid(true);
            result.setMessage("All documents validated!");
        } else {
            result.setValid(false);
            result.setMessage("Missing: " + String.join(", ", result.getMissingDocuments()));
        }
        
        return result;
    }
    
    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.length() == 0 || s2.length() == 0) return 0.0;

        Set<String> set1 = new HashSet<>(Arrays.asList(s1.split("[\\s,_-]+")));
        Set<String> set2 = new HashSet<>(Arrays.asList(s2.split("[\\s,_-]+")));

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }
    
    public List<String> getSupportedDocumentTypes() {
        return new ArrayList<>(DOCUMENT_KEYWORDS.keySet());
    }
    
    public List<String> getKeywordsForDocumentType(String documentType) {
        return DOCUMENT_KEYWORDS.getOrDefault(documentType.toUpperCase(), new ArrayList<>());
    }
}

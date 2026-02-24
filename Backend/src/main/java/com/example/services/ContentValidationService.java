package com.example.services;

import com.example.dto.DocumentValidationDetail;
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


    //  CONTENT-BASED VALIDATION (COMPREHENSIVE - DOCUMENT SPECIFIC)
    
    public ValidationResult validateDocumentContent(List<String> requiredDocuments, MultipartFile[] files) {
        
        logger.info("=== Starting COMPREHENSIVE CONTENT-BASED validation (Document-Specific) ===");
        
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
        
        // Extract text from files
        Map<String, String> extractedContent = extractTextFromFiles(files);
        
        // Validate each required document using comprehensive validation
        for (String requiredDoc : requiredDocuments) {
            boolean found = findDocumentComprehensively(requiredDoc, extractedContent, result);
            
            if (!found) {
                result.getMissingDocuments().add(requiredDoc);
            }
        }
        
        // Set final message
        if (result.getMissingDocuments().isEmpty()) {
            result.setValid(true);
            result.setMessage("All required documents validated via comprehensive content analysis!");
        } else {
            result.setValid(false);
            result.setMessage("Missing or invalid: " + String.join(", ", result.getMissingDocuments()));
        }
        
        // Add warnings for files with extraction issues
        addWarningsForRequiredDocs(extractedContent, result, requiredDocuments);
        
        return result;
    }

    /**
     * Comprehensive document validation using document-specific templates
     * Validates that the document contains expected fields for that document type
     */
    private boolean findDocumentComprehensively(String requiredDoc, Map<String, String> extractedContent, ValidationResult result) {
        String requiredDocLower = requiredDoc.toLowerCase().trim();
        boolean found = false;

        List<String> keywords = new ArrayList<>(keywordMatcher.getKeywordsForDocument(requiredDocLower));

        // Prefer files whose *filename* suggests the doc, but don't require it (users often upload as scan_001.pdf).
        List<Map.Entry<String, String>> preferred = new ArrayList<>();
        List<Map.Entry<String, String>> others = new ArrayList<>();

        for (Map.Entry<String, String> entry : extractedContent.entrySet()) {
            String fileNameLower = entry.getKey().toLowerCase();
            boolean filenameMatch = fileNameLower.contains(requiredDocLower);
            if (!filenameMatch) {
                for (String keyword : keywords) {
                    if (keyword != null && fileNameLower.contains(keyword.toLowerCase())) {
                        filenameMatch = true;
                        break;
                    }
                }
            }
            (filenameMatch ? preferred : others).add(entry);
        }

        List<Map.Entry<String, String>> ordered = new ArrayList<>(preferred);
        ordered.addAll(others);

        for (Map.Entry<String, String> entry : ordered) {
            String content = entry.getValue();

            // Check for empty content
            if (content == null || content.trim().isEmpty()
                    || content.toLowerCase().startsWith("error")
                    || content.equals("image_pdf_fallback")) {
                continue;
            }

            KeywordMatcher.DocumentValidationResult validationResult =
                    keywordMatcher.validateDocumentComprehensively(requiredDocLower, content);

            if (validationResult.isValid()) {
                found = true;
                String matchInfo = requiredDoc;
                if (validationResult.getDocumentNumber() != null) {
                    matchInfo += " [" + validationResult.getDocumentType() + ": " + validationResult.getDocumentNumber() + "]";
                }
                matchInfo += " [Score: " + String.format("%.0f", validationResult.getValidationScore()) + "%]";
                matchInfo += " -> " + entry.getKey();

                result.getMatchedDocuments().add(matchInfo);

                DocumentValidationDetail detail = new DocumentValidationDetail();
                detail.setDocumentType(validationResult.getDocumentType());
                detail.setValid(true);
                detail.setDocumentNumber(validationResult.getDocumentNumber());
                detail.setValidationScore(validationResult.getValidationScore());
                detail.setValidatedFields(validationResult.getValidatedFields());
                detail.setMissingFields(validationResult.getMissingFields());
                result.addDocumentDetail(entry.getKey(), detail);

                if (!validationResult.getValidatedFields().isEmpty()) {
                    result.getWarnings().add(entry.getKey() + ": Validated fields: " +
                            String.join(", ", validationResult.getValidatedFields()));
                }

                logger.info("Document '{}' validated comprehensively in {} (Score: {:.0f}%)",
                        requiredDoc, entry.getKey(), validationResult.getValidationScore());
                break;
            } else {
                logger.info("Document '{}' failed comprehensive validation in {}: {}",
                        requiredDoc, entry.getKey(), validationResult.getErrorMessage());

                DocumentValidationDetail detail = new DocumentValidationDetail();
                detail.setDocumentType(requiredDoc);
                detail.setValid(false);
                detail.setValidationScore(validationResult.getValidationScore());
                detail.setValidatedFields(validationResult.getValidatedFields());
                detail.setMissingFields(validationResult.getMissingFields());
                detail.setErrorMessage(validationResult.getErrorMessage());
                result.addDocumentDetail(entry.getKey(), detail);
            }
        }

        return found;
    }

    // Add warnings only for required document files
    private void addWarningsForRequiredDocs(Map<String, String> extractedContent, 
                                           ValidationResult result,
                                           List<String> requiredDocuments) {
        // Build keywords from required documents
        Set<String> requiredKeywords = new HashSet<>();
        for (String doc : requiredDocuments) {
            requiredKeywords.add(doc.toLowerCase());
        }
        
        List<String> warnings = new ArrayList<>();
        for (Map.Entry<String, String> entry : extractedContent.entrySet()) {
            String fileName = entry.getKey().toLowerCase();
            String content = entry.getValue();
            
            // Check if this file could be a required document based on filename
            boolean couldBeRequiredDoc = false;
            for (String keyword : requiredKeywords) {
                if (fileName.contains(keyword)) {
                    couldBeRequiredDoc = true;
                    break;
                }
            }
            
            // Only warn if it's likely a required document
            if (couldBeRequiredDoc && (content == null || content.trim().isEmpty() || 
                content.startsWith("error") || content.equals("image_pdf_fallback"))) {
                warnings.add(entry.getKey());
            }
        }
        
        if (!warnings.isEmpty()) {
            result.getWarnings().add("OCR extraction issues for: " + String.join(", ", warnings));
        }
    }

    private boolean findDocumentInContent(String requiredDoc, Map<String, String> extractedContent, ValidationResult result) {
        String requiredDocLower = requiredDoc.toLowerCase().trim();
        boolean found = false;

        boolean isPanCard = requiredDocLower.contains("pan");
        boolean isAadhaar = requiredDocLower.contains("aadhaar") || requiredDocLower.contains("aadhar") || requiredDocLower.contains("uid");
        boolean isGST = requiredDocLower.contains("gst") || requiredDocLower.contains("gstin");
        boolean isIncomeTax = requiredDocLower.contains("income tax") || requiredDocLower.contains("itr") || requiredDocLower.contains("tax");
        boolean isExperience = requiredDocLower.contains("experience");
        boolean isCompanyReg = requiredDocLower.contains("company") || requiredDocLower.contains("incorporation") || requiredDocLower.contains("roc");
        boolean isInsurance = requiredDocLower.contains("insurance");

        // Get keywords ONLY for the required document type
        List<String> keywords = new ArrayList<>(keywordMatcher.getKeywordsForDocument(requiredDocLower));
        if (!keywords.contains(requiredDocLower)) {
            keywords.add(requiredDocLower);
        }

        for (Map.Entry<String, String> entry : extractedContent.entrySet()) {
            String fileName = entry.getKey().toLowerCase();
            String content = entry.getValue();
            String contentLower = content != null ? content.toLowerCase() : "";

            // Check for empty content
            if (content == null || content.trim().isEmpty() || content.toLowerCase().startsWith("error") || content.equals("image_pdf_fallback")) {
                continue;
            }

            // PAN Card validation - only for PAN required documents
            if (isPanCard) {
                // First try PAN number pattern
                String foundPanNumber = keywordMatcher.validatePanCardInContent(content);
                if (foundPanNumber != null) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [PAN: " + foundPanNumber + "] -> " + entry.getKey());
                    logger.info("PAN card validated via PAN number format '{}' in {}", foundPanNumber, entry.getKey());
                    break;
                }
                // Then try PAN keywords only (not other document keywords)
                if (keywordMatcher.checkContentMatch(requiredDocLower, keywords, fileName, contentLower)) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [KEYWORD] -> " + entry.getKey());
                    logger.info("PAN card validated via keyword in {}", entry.getKey());
                    break;
                }
            }
            // Aadhaar validation - only for Aadhaar required documents
            else if (isAadhaar) {
                String foundAadhaar = keywordMatcher.validateAadharCardInContent(content);
                if (foundAadhaar != null) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [AADHAAR: " + foundAadhaar + "] -> " + entry.getKey());
                    logger.info("Aadhaar validated in {}", entry.getKey());
                    break;
                }
                if (keywordMatcher.checkContentMatch(requiredDocLower, keywords, fileName, contentLower)) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [KEYWORD] -> " + entry.getKey());
                    break;
                }
            }
            // GST validation - only for GST required documents
            else if (isGST) {
                String foundGST = keywordMatcher.validateGSTInContent(content);
                if (foundGST != null) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [GST: " + foundGST + "] -> " + entry.getKey());
                    logger.info("GST validated in {}", entry.getKey());
                    break;
                }
                if (keywordMatcher.checkContentMatch(requiredDocLower, keywords, fileName, contentLower)) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [KEYWORD] -> " + entry.getKey());
                    break;
                }
            }
            // Income Tax validation
            else if (isIncomeTax) {
                if (keywordMatcher.validateIncomeTaxInContent(content) != null) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [KEYWORD] -> " + entry.getKey());
                    break;
                }
                if (keywordMatcher.checkContentMatch(requiredDocLower, keywords, fileName, contentLower)) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [KEYWORD] -> " + entry.getKey());
                    break;
                }
            }
            // Experience validation
            else if (isExperience) {
                if (keywordMatcher.validateExperienceCertificateInContent(content) != null) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [KEYWORD] -> " + entry.getKey());
                    break;
                }
                if (keywordMatcher.checkContentMatch(requiredDocLower, keywords, fileName, contentLower)) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [KEYWORD] -> " + entry.getKey());
                    break;
                }
            }
            // Company Registration validation
            else if (isCompanyReg) {
                if (keywordMatcher.validateCompanyRegistrationInContent(content) != null) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [KEYWORD] -> " + entry.getKey());
                    break;
                }
                if (keywordMatcher.checkContentMatch(requiredDocLower, keywords, fileName, contentLower)) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [KEYWORD] -> " + entry.getKey());
                    break;
                }
            }
            // Insurance validation
            else if (isInsurance) {
                if (keywordMatcher.validateInsuranceInContent(content) != null) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [KEYWORD] -> " + entry.getKey());
                    break;
                }
                if (keywordMatcher.checkContentMatch(requiredDocLower, keywords, fileName, contentLower)) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [KEYWORD] -> " + entry.getKey());
                    break;
                }
            }
            // For other document types, use generic keyword matching
            else {
                if (keywordMatcher.checkContentMatch(requiredDocLower, keywords, fileName, contentLower)) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [CONTENT] -> " + entry.getKey());
                    logger.info("Content matched: '{}' in {}", requiredDoc, entry.getKey());
                    break;
                }
            }
        }

        return found;
    }

    //  RULE-BASED VALIDATION (SIMPLIFIED)
   
    public ValidationResult validateWithRules(List<String> requiredDocuments, MultipartFile[] files) {

        logger.info("=== Starting RULE-BASED validation (Simplified) ===");

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

        //  Step 0 → Duplicate Check
        checkForDuplicates(files, result);

        //  Step 1 → OCR Extraction
        Map<String, String> extractedContents = extractTextFromFiles(files);

        //  Step 2 → EXCLUSIVE classification (no overlap)
        // Each uploaded file can satisfy ONLY ONE document type.
        Map<String, Classification> fileClassification = classifyExtractedContents(extractedContents);

        // Build type -> best matching file mapping (highest score)
        Map<String, List<Classification>> byType = new HashMap<>();
        for (Classification c : fileClassification.values()) {
            byType.computeIfAbsent(c.type, k -> new ArrayList<>()).add(c);
        }
        for (List<Classification> list : byType.values()) {
            list.sort((a, b) -> Integer.compare(b.score, a.score));
        }

        //  Step 3 → Validate required docs using classified files (no overlap)
        for (String requiredDoc : requiredDocuments) {
            String requiredType = normalizeRequiredDocType(requiredDoc);

            if ("UNKNOWN".equals(requiredType)) {
                result.getMissingDocuments().add(requiredDoc);
                result.getWarnings().add("Unknown required document type: " + requiredDoc);
                logger.warn("Unknown required document type: {}", requiredDoc);
                continue;
            }

            List<Classification> candidates = byType.getOrDefault(requiredType, new ArrayList<>());
            if (candidates.isEmpty()) {
                result.getMissingDocuments().add(requiredDoc);
                logger.warn("✗ Document NOT FOUND (type={}): {}", requiredType, requiredDoc);
                continue;
            }

            // Use top candidate for this type
            Classification best = candidates.get(0);
            String matchInfo = requiredDoc + " -> " + best.fileName +
                    " (" + best.type + ", score=" + best.score +
                    (best.documentNumber != null ? ", id=" + best.documentNumber : "") + ")";
            result.getMatchedDocuments().add(matchInfo);
            logger.info("✓ Document VALIDATED (type={}): {} via {}", requiredType, requiredDoc, best.fileName);
        }

        //  Step 4 → OCR Failure Warnings (Only for required documents)
        addWarningsForUnvalidatedFiles(extractedContents, result, requiredDocuments);

        // Warn about ambiguous/unknown classifications
        for (Classification c : fileClassification.values()) {
            if ("UNKNOWN".equals(c.type)) {
                result.getWarnings().add(c.fileName + ": Could not classify document (OCR text present but no strong match)");
            } else if (c.ambiguous) {
                result.getWarnings().add(c.fileName + ": Classification ambiguous, picked " + c.type + " (score=" + c.score + ")");
            }
        }

        // Final Message
        setValidationMessage(result, requiredDocuments.size());

        return result;
    }

    /**
     * Normalize UI/required document names into canonical types.
     * Ensures consistent, non-overlapping validation.
     */
    private String normalizeRequiredDocType(String requiredDoc) {
        if (requiredDoc == null) return "UNKNOWN";
        String docLower = requiredDoc.toLowerCase();

        // IMPORTANT: use word-boundary matching to avoid matching "comPANY" as "PAN"
        if (containsWord(docLower, "pan")) return "PAN";
        if (docLower.contains("aadhaar") || docLower.contains("aadhar") || docLower.contains("uid")) return "AADHAAR";
        if (docLower.contains("gst")) return "GST";
        if (docLower.contains("income tax") || docLower.contains("itr") || docLower.contains("tax clearance")) return "INCOME_TAX";
        if (docLower.contains("experience")) return "EXPERIENCE";
        if (docLower.contains("company") || docLower.contains("incorporation") || docLower.contains("roc")) return "COMPANY_REG";
        if (docLower.contains("insurance")) return "INSURANCE";

        return "UNKNOWN";
    }

    private boolean containsWord(String text, String word) {
        if (text == null || word == null || word.isBlank()) return false;
        return java.util.regex.Pattern
                .compile("\\b" + java.util.regex.Pattern.quote(word) + "\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(text)
                .find();
    }

    private static class Classification {
        final String fileName;
        final String type; // PAN, AADHAAR, GST, INCOME_TAX, EXPERIENCE, COMPANY_REG, INSURANCE, UNKNOWN
        final int score;   // 0-100
        final String documentNumber; // PAN/Aadhaar/GSTIN/CIN if found
        final boolean ambiguous;

        private Classification(String fileName, String type, int score, String documentNumber, boolean ambiguous) {
            this.fileName = fileName;
            this.type = type;
            this.score = score;
            this.documentNumber = documentNumber;
            this.ambiguous = ambiguous;
        }
    }

    /**
     * Classify each extracted file into exactly one type (no overlap).
     * Uses strong patterns + context-aware validators in KeywordMatcher.
     */
    private Map<String, Classification> classifyExtractedContents(Map<String, String> extractedContents) {
        Map<String, Classification> out = new HashMap<>();

        for (Map.Entry<String, String> entry : extractedContents.entrySet()) {
            String fileName = entry.getKey();
            String content = entry.getValue();

            if (content == null || content.startsWith("IMAGE_PDF_FALLBACK") || content.isBlank()) {
                out.put(fileName, new Classification(fileName, "UNKNOWN", 0, null, false));
                continue;
            }

            // Score each type
            Score pan = scorePan(content);
            Score aadhaar = scoreAadhaar(content);
            Score gst = scoreGst(content);
            Score incomeTax = scoreIncomeTax(content);
            Score experience = scoreExperience(content);
            Score companyReg = scoreCompanyReg(content);
            Score insurance = scoreInsurance(content);

            List<Score> scores = Arrays.asList(pan, aadhaar, gst, incomeTax, experience, companyReg, insurance);
            scores.sort((a, b) -> Integer.compare(b.score, a.score));

            Score best = scores.get(0);
            Score second = scores.get(1);

            // Require a minimum score; otherwise unknown
            if (best.score < 50) {
                out.put(fileName, new Classification(fileName, "UNKNOWN", best.score, best.documentNumber, false));
                continue;
            }

            // Mark ambiguous if close scores
            boolean ambiguous = second.score >= 50 && (best.score - second.score) <= 10;

            out.put(fileName, new Classification(fileName, best.type, best.score, best.documentNumber, ambiguous));
        }

        return out;
    }

    private static class Score {
        final String type;
        final int score;
        final String documentNumber;

        private Score(String type, int score, String documentNumber) {
            this.type = type;
            this.score = score;
            this.documentNumber = documentNumber;
        }
    }

    private Score scorePan(String content) {
        String pan = keywordMatcher.validatePanCardInContent(content);
        if (pan == null) return new Score("PAN", 0, null);
        if ("PAN_KEYWORD_FOUND".equals(pan)) return new Score("PAN", 60, null);
        return new Score("PAN", 90, pan);
    }

    private Score scoreAadhaar(String content) {
        String a = keywordMatcher.validateAadharCardInContent(content);
        if (a == null) return new Score("AADHAAR", 0, null);
        if ("AADHAAR_KEYWORD_FOUND".equals(a)) return new Score("AADHAAR", 60, null);
        return new Score("AADHAAR", 90, a);
    }

    private Score scoreGst(String content) {
        String gst = keywordMatcher.validateGSTInContent(content);
        if (gst == null) return new Score("GST", 0, null);
        if ("GST_KEYWORD_FOUND".equals(gst)) return new Score("GST", 60, null);
        return new Score("GST", 90, gst);
    }

    private Score scoreIncomeTax(String content) {
        String it = keywordMatcher.validateIncomeTaxInContent(content);
        if (it == null) return new Score("INCOME_TAX", 0, null);

        // Boost if strong ITR/AY context
        String lower = content.toLowerCase();
        int score = 60;
        if (lower.contains("assessment year") || lower.contains("return of income") || lower.contains("itr")) {
            score = 80;
        }
        return new Score("INCOME_TAX", score, null);
    }

    private Score scoreExperience(String content) {
        String exp = keywordMatcher.validateExperienceCertificateInContent(content);
        if (exp == null) return new Score("EXPERIENCE", 0, null);
        return new Score("EXPERIENCE", 70, null);
    }

    private Score scoreCompanyReg(String content) {
        String cr = keywordMatcher.validateCompanyRegistrationInContent(content);
        if (cr == null) return new Score("COMPANY_REG", 0, null);
        if ("COMPANY_REG_FOUND".equals(cr)) return new Score("COMPANY_REG", 70, null);
        // CIN returned
        return new Score("COMPANY_REG", 90, cr);
    }

    private Score scoreInsurance(String content) {
        String ins = keywordMatcher.validateInsuranceInContent(content);
        if (ins == null) return new Score("INSURANCE", 0, null);
        return new Score("INSURANCE", 70, null);
    }

   
    //  OCR TEXT EXTRACTION
    private Map<String, String> extractTextFromFiles(MultipartFile[] files) {

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

    
    //  DOCUMENT VALIDATION (ONLY REQUIRED DOCS - DOCUMENT SPECIFIC)
    
    private boolean findDocumentWithRules(String requiredDoc,
                                          Map<String, String> extractedContents,
                                          ValidationResult result) {

        String docLower = requiredDoc.toLowerCase();

        // Determine document type
        boolean isPan = docLower.contains("pan");
        boolean isAadhaar = docLower.contains("aadhaar") || docLower.contains("aadhar") || docLower.contains("uid");
        boolean isGST = docLower.contains("gst") || docLower.contains("gstin");
        boolean isIncomeTax = docLower.contains("income tax") || docLower.contains("itr") || docLower.contains("tax");
        boolean isExperience = docLower.contains("experience");
        boolean isCompanyReg = docLower.contains("company") || docLower.contains("incorporation") || docLower.contains("roc");
        boolean isInsurance = docLower.contains("insurance");

        // Get keywords ONLY for this specific document type
        List<String> keywords = new ArrayList<>(keywordMatcher.getKeywordsForDocument(docLower));
        if (!keywords.contains(docLower)) {
            keywords.add(docLower);
        }

        for (Map.Entry<String, String> entry : extractedContents.entrySet()) {

            String fileName = entry.getKey();
            String content = entry.getValue();

            if (content == null || content.startsWith("IMAGE_PDF_FALLBACK"))
                continue;

            // PAN validation - ONLY if PAN is required
            if (isPan) {
                String pan = keywordMatcher.validatePanCardInContent(content);
                if (pan != null) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName + " (PAN: " + pan + ")");
                    return true;
                }
                // Also check keyword match
                if (keywordMatcher.checkContentMatch(docLower, keywords, fileName.toLowerCase(), content.toLowerCase())) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName);
                    return true;
                }
                continue; // Don't check other document types
            }

            // Aadhaar validation - ONLY if Aadhaar is required
            if (isAadhaar) {
                String aadhaar = keywordMatcher.validateAadharCardInContent(content);
                if (aadhaar != null) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName + " (Aadhaar: " + aadhaar + ")");
                    return true;
                }
                if (keywordMatcher.checkContentMatch(docLower, keywords, fileName.toLowerCase(), content.toLowerCase())) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName);
                    return true;
                }
                continue;
            }

            // GST validation - ONLY if GST is required
            if (isGST) {
                String gst = keywordMatcher.validateGSTInContent(content);
                if (gst != null) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName + " (GSTIN: " + gst + ")");
                    return true;
                }
                if (keywordMatcher.checkContentMatch(docLower, keywords, fileName.toLowerCase(), content.toLowerCase())) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName);
                    return true;
                }
                continue;
            }

            // Income Tax validation - ONLY if Income Tax is required
            if (isIncomeTax) {
                if (keywordMatcher.validateIncomeTaxInContent(content) != null) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName);
                    return true;
                }
                if (keywordMatcher.checkContentMatch(docLower, keywords, fileName.toLowerCase(), content.toLowerCase())) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName);
                    return true;
                }
                continue;
            }

            // Experience validation - ONLY if Experience is required
            if (isExperience) {
                if (keywordMatcher.validateExperienceCertificateInContent(content) != null) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName);
                    return true;
                }
                if (keywordMatcher.checkContentMatch(docLower, keywords, fileName.toLowerCase(), content.toLowerCase())) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName);
                    return true;
                }
                continue;
            }

            // Company Registration validation - ONLY if Company Reg is required
            if (isCompanyReg) {
                if (keywordMatcher.validateCompanyRegistrationInContent(content) != null) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName);
                    return true;
                }
                if (keywordMatcher.checkContentMatch(docLower, keywords, fileName.toLowerCase(), content.toLowerCase())) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName);
                    return true;
                }
                continue;
            }

            // Insurance validation - ONLY if Insurance is required
            if (isInsurance) {
                if (keywordMatcher.validateInsuranceInContent(content) != null) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName);
                    return true;
                }
                if (keywordMatcher.checkContentMatch(docLower, keywords, fileName.toLowerCase(), content.toLowerCase())) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName);
                    return true;
                }
                continue;
            }
        }

        return false;
    }

    //  DUPLICATE CHECK
   
    private void checkForDuplicates(MultipartFile[] files, ValidationResult result) {
        List<String> duplicates = duplicateDetector.getDuplicateFileNames(files);
        if (!duplicates.isEmpty()) {
            result.getDuplicateDocuments().addAll(duplicates);
            logger.warn("Duplicate files detected: {}", duplicates);
        }
    }

    //  WARNINGS FOR OCR FAILURES (Only for required documents)
    
    private void addWarningsForUnvalidatedFiles(Map<String, String> extractedContents,
                                                ValidationResult result,
                                                List<String> requiredDocuments) {

        // Get the set of required document keywords to check
        Set<String> requiredDocKeywords = new HashSet<>();
        for (String doc : requiredDocuments) {
            requiredDocKeywords.add(doc.toLowerCase());
            // Add common variations
            if (containsWord(doc.toLowerCase(), "pan")) {
                requiredDocKeywords.add("pan");
            }
            if (doc.toLowerCase().contains("aadhaar") || doc.toLowerCase().contains("adhar")) {
                requiredDocKeywords.add("aadhaar");
                requiredDocKeywords.add("aadhar");
            }
            if (doc.toLowerCase().contains("gst")) {
                requiredDocKeywords.add("gst");
            }
            if (doc.toLowerCase().contains("income tax") || doc.toLowerCase().contains("itr")) {
                requiredDocKeywords.add("income tax");
                requiredDocKeywords.add("itr");
            }
            if (doc.toLowerCase().contains("experience")) {
                requiredDocKeywords.add("experience");
            }
            if (doc.toLowerCase().contains("company")) {
                requiredDocKeywords.add("company");
            }
            if (doc.toLowerCase().contains("insurance")) {
                requiredDocKeywords.add("insurance");
            }
        }

        List<String> failedFiles = new ArrayList<>();

        for (Map.Entry<String, String> entry : extractedContents.entrySet()) {
            if (entry.getValue().startsWith("IMAGE_PDF_FALLBACK")) {
                // Only warn about files that could be related to required documents
                String fileNameLower = entry.getKey().toLowerCase();
                boolean isRequiredDocFile = false;
                
                for (String keyword : requiredDocKeywords) {
                    if (fileNameLower.contains(keyword)) {
                        isRequiredDocFile = true;
                        break;
                    }
                }
                
                // Only add warning if this file could be a required document
                if (isRequiredDocFile) {
                    failedFiles.add(entry.getKey());
                }
            }
        }

        if (!failedFiles.isEmpty()) {
            result.getWarnings().add("OCR failed for: " + String.join(", ", failedFiles));
        }
    }


    //  FINAL MESSAGE
    
    private void setValidationMessage(ValidationResult result, int totalRequired) {

        if (result.getMissingDocuments().isEmpty()) {
            result.setValid(true);
            result.setMessage("All required documents validated successfully.");
        } else {
            result.setValid(false);
            result.setMessage("Missing documents: " +
                    String.join(", ", result.getMissingDocuments()));
        }
    }
}
package com.example.services;

import com.example.dto.DocumentValidationDetail;
import com.example.dto.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DocumentFinder {

    private static final Logger logger = LoggerFactory.getLogger(DocumentFinder.class);

    @Autowired
    private KeywordMatcher keywordMatcher;

    public boolean findDocumentComprehensively(String requiredDoc, 
                                                 Map<String, String> extractedContent, 
                                                 ValidationResult result) {
        String requiredDocLower = requiredDoc.toLowerCase().trim();
        boolean found = false;

        List<String> keywords = new ArrayList<>(keywordMatcher.getKeywordsForDocument(requiredDocLower));

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

            boolean isValidContent = content != null && !content.trim().isEmpty();
            boolean isNotError = !content.toLowerCase().startsWith("error");
            boolean isNotFallback = !content.equals("image_pdf_fallback");

            if (!isValidContent || !isNotError || !isNotFallback) {
                continue;
            }

            KeywordMatcher.DocumentValidationResult validationResult =
                    keywordMatcher.validateDocumentComprehensively(requiredDocLower, content);

            if (validationResult.isValid()) {
                found = true;
                String matchInfo = requiredDoc;
                if (validationResult.getDocumentNumber() != null) {
                    String docType = validationResult.getDocumentType();
                    String docNumber = validationResult.getDocumentNumber();
                    matchInfo += " [" + docType + ": " + docNumber + "]";
                }
                double scorePercent = validationResult.getValidationScore();
                matchInfo += " [Score: " + String.format("%.0f", scorePercent) + "%]";
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

    public boolean findDocumentInContent(String requiredDoc, 
                                          Map<String, String> extractedContent, 
                                          ValidationResult result) {
        String requiredDocLower = requiredDoc.toLowerCase().trim();
        boolean found = false;

        boolean isPanCard = requiredDocLower.contains("pan");
        boolean isAadhaar = requiredDocLower.contains("aadhaar") || requiredDocLower.contains("aadhar") || requiredDocLower.contains("uid");
        boolean isGST = requiredDocLower.contains("gst") || requiredDocLower.contains("gstin");
        boolean isIncomeTax = requiredDocLower.contains("income tax") || requiredDocLower.contains("itr") || requiredDocLower.contains("tax");
        boolean isExperience = requiredDocLower.contains("experience");
        boolean isCompanyReg = requiredDocLower.contains("company") || requiredDocLower.contains("incorporation") || requiredDocLower.contains("roc");
        boolean isInsurance = requiredDocLower.contains("insurance");

        List<String> keywords = new ArrayList<>(keywordMatcher.getKeywordsForDocument(requiredDocLower));
        if (!keywords.contains(requiredDocLower)) {
            keywords.add(requiredDocLower);
        }

        for (Map.Entry<String, String> entry : extractedContent.entrySet()) {
            String fileName = entry.getKey().toLowerCase();
            String content = entry.getValue();
            String contentLower = content != null ? content.toLowerCase() : "";

            boolean hasValidContent = content != null && !content.trim().isEmpty();
            boolean isNotError = !content.toLowerCase().startsWith("error");
            boolean isNotFallback = !content.equals("image_pdf_fallback");

            if (!hasValidContent || !isNotError || !isNotFallback) {
                continue;
            }

            if (isPanCard) {
                String foundPanNumber = keywordMatcher.validatePanCardInContent(content);
                if (foundPanNumber != null) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [PAN: " + foundPanNumber + "] -> " + entry.getKey());
                    logger.info("PAN card validated via PAN number format '{}' in {}", foundPanNumber, entry.getKey());
                    break;
                }

                if (keywordMatcher.checkContentMatch(requiredDocLower, keywords, fileName, contentLower)) {
                    found = true;
                    result.getMatchedDocuments().add(requiredDoc + " [KEYWORD] -> " + entry.getKey());
                    logger.info("PAN card validated via keyword in {}", entry.getKey());
                    break;
                }
            }

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

    public boolean findDocumentWithRules(String requiredDoc,
                                          Map<String, String> extractedContents,
                                          ValidationResult result) {

        String docLower = requiredDoc.toLowerCase();

        boolean isPan = docLower.contains("pan");
        boolean isAadhaar = docLower.contains("aadhaar") || docLower.contains("aadhar") || docLower.contains("uid");
        boolean isGST = docLower.contains("gst") || docLower.contains("gstin");
        boolean isIncomeTax = docLower.contains("income tax") || docLower.contains("itr") || docLower.contains("tax");
        boolean isExperience = docLower.contains("experience");
        boolean isCompanyReg = docLower.contains("company") || docLower.contains("incorporation") || docLower.contains("roc");
        boolean isInsurance = docLower.contains("insurance");

        List<String> keywords = new ArrayList<>(keywordMatcher.getKeywordsForDocument(docLower));
        if (!keywords.contains(docLower)) {
            keywords.add(docLower);
        }

        for (Map.Entry<String, String> entry : extractedContents.entrySet()) {

            String fileName = entry.getKey();
            String content = entry.getValue();

            if (content == null || content.startsWith("IMAGE_PDF_FALLBACK"))
                continue;

            if (isPan) {
                String pan = keywordMatcher.validatePanCardInContent(content);
                if (pan != null) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName + " (PAN: " + pan + ")");
                    return true;
                }

                if (keywordMatcher.checkContentMatch(docLower, keywords, fileName.toLowerCase(), content.toLowerCase())) {
                    result.getMatchedDocuments().add(requiredDoc + " -> " + fileName);
                    return true;
                }
                continue;
            }

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
}

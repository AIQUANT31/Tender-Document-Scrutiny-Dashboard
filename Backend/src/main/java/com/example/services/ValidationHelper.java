package com.example.services;

import com.example.dto.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Component
public class ValidationHelper {

    private static final Logger logger = LoggerFactory.getLogger(ValidationHelper.class);

    @Autowired
    private DuplicateDetector duplicateDetector;

    public String normalizeRequiredDocType(String requiredDoc) {
        if (requiredDoc == null) return "UNKNOWN";
        String docLower = requiredDoc.toLowerCase();

        if (containsWord(docLower, "pan")) return "PAN";
        if (isAadhaarDocument(docLower)) return "AADHAAR";
        if (docLower.contains("gst")) return "GST";
        if (containsAny(docLower, "income tax", "itr", "tax clearance")) return "INCOME_TAX";
        if (docLower.contains("experience")) return "EXPERIENCE";
        if (isCompanyRegistrationDocument(docLower)) return "COMPANY_REG";
        if (docLower.contains("insurance")) return "INSURANCE";

        return "UNKNOWN";
    }

    public boolean isAadhaarDocument(String docLower) {
        return docLower.contains("aadhaar") || docLower.contains("aadhar") || docLower.contains("uid");
    }

    public boolean isCompanyRegistrationDocument(String docLower) {
        return docLower.contains("company") || docLower.contains("incorporation") || docLower.contains("roc");
    }

    public boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    public boolean containsWord(String text, String word) {
        if (text == null || word == null || word.isBlank()) return false;
        return java.util.regex.Pattern
                .compile("\\b" + java.util.regex.Pattern.quote(word) + "\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(text)
                .find();
    }

    public void addWarningsForRequiredDocs(Map<String, String> extractedContent,
                                             ValidationResult result,
                                             List<String> requiredDocuments) {

        Set<String> requiredKeywords = new HashSet<>();
        for (String doc : requiredDocuments) {
            requiredKeywords.add(doc.toLowerCase());
        }

        List<String> warnings = new ArrayList<>();
        for (Map.Entry<String, String> entry : extractedContent.entrySet()) {
            String fileName = entry.getKey().toLowerCase();
            String content = entry.getValue();

            boolean couldBeRequiredDoc = false;
            for (String keyword : requiredKeywords) {
                if (fileName.contains(keyword)) {
                    couldBeRequiredDoc = true;
                    break;
                }
            }

            boolean hasOcrIssue = (content == null || content.trim().isEmpty()
                || content.startsWith("error") || content.equals("image_pdf_fallback"));

            if (couldBeRequiredDoc && hasOcrIssue) {
                warnings.add(entry.getKey());
            }
        }

        if (!warnings.isEmpty()) {
            result.getWarnings().add("OCR extraction issues for: " + String.join(", ", warnings));
        }
    }

    public void addWarningsForUnvalidatedFiles(Map<String, String> extractedContents,
                                                 ValidationResult result,
                                                 List<String> requiredDocuments) {

        Set<String> requiredDocKeywords = new HashSet<>();
        for (String doc : requiredDocuments) {
            requiredDocKeywords.add(doc.toLowerCase());

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

                String fileNameLower = entry.getKey().toLowerCase();
                boolean isRequiredDocFile = false;

                for (String keyword : requiredDocKeywords) {
                    if (fileNameLower.contains(keyword)) {
                        isRequiredDocFile = true;
                        break;
                    }
                }

                if (isRequiredDocFile) {
                    failedFiles.add(entry.getKey());
                }
            }
        }

        if (!failedFiles.isEmpty()) {
            result.getWarnings().add("OCR failed for: " + String.join(", ", failedFiles));
        }
    }

    public void checkForDuplicates(MultipartFile[] files, ValidationResult result) {
        List<String> duplicates = duplicateDetector.getDuplicateFileNames(files);
        if (!duplicates.isEmpty()) {
            result.getDuplicateDocuments().addAll(duplicates);
            logger.warn("Duplicate files detected: {}", duplicates);
        }
    }

    public void setValidationMessage(ValidationResult result, int totalRequired) {

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

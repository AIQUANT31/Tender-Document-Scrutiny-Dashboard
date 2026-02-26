package com.example.services.validation;

import com.example.dto.ValidationResult;
import com.example.services.validation.ContentValidationModels.Classification;
import com.example.services.document.TextExtractor;
import com.example.services.document.DocumentFinder;
import com.example.services.document.DocumentClassifier;
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
    private TextExtractor textExtractor;

    @Autowired
    private DocumentFinder documentFinder;

    @Autowired
    private DocumentClassifier documentClassifier;

    @Autowired
    private ValidationHelper validationHelper;

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

        Map<String, String> extractedContent = textExtractor.extractTextFromFiles(files);

        for (String requiredDoc : requiredDocuments) {
            boolean found = documentFinder.findDocumentComprehensively(requiredDoc, extractedContent, result);

            if (!found) {
                result.getMissingDocuments().add(requiredDoc);
            }
        }

        if (result.getMissingDocuments().isEmpty()) {
            result.setValid(true);
            result.setMessage("All required documents validated via comprehensive content analysis!");
        } else {
            result.setValid(false);
            result.setMessage("Missing or invalid: " + String.join(", ", result.getMissingDocuments()));
        }

        validationHelper.addWarningsForRequiredDocs(extractedContent, result, requiredDocuments);

        return result;
    }

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

        validationHelper.checkForDuplicates(files, result);

        Map<String, String> extractedContents = textExtractor.extractTextFromFiles(files);

        Map<String, Classification> fileClassification = documentClassifier.classifyExtractedContents(extractedContents);

        Map<String, List<Classification>> byType = documentClassifier.getClassificationsByType(fileClassification);

        for (String requiredDoc : requiredDocuments) {
            String requiredType = validationHelper.normalizeRequiredDocType(requiredDoc);

            if ("UNKNOWN".equals(requiredType)) {
                result.getMissingDocuments().add(requiredDoc);
                result.getWarnings().add("Unknown required document type: " + requiredDoc);
                logger.warn("Unknown required document type: {}", requiredDoc);
                continue;
            }

            List<Classification> candidates = byType.getOrDefault(requiredType, new ArrayList<>());
            if (candidates.isEmpty()) {
                result.getMissingDocuments().add(requiredDoc);
                logger.warn("Document NOT FOUND (type={}): {}", requiredType, requiredDoc);
                continue;
            }

            Classification best = candidates.get(0);
            String typeInfo = best.type;
            String scoreInfo = "score=" + best.score;
            String idInfo = (best.documentNumber != null) ? ", id=" + best.documentNumber : "";

            String matchInfo = requiredDoc + " -> " + best.fileName +
                    " (" + typeInfo + ", " + scoreInfo + idInfo + ")";
            result.getMatchedDocuments().add(matchInfo);
            logger.info("Document VALIDATED (type={}): {} via {}", requiredType, requiredDoc, best.fileName);
        }

        validationHelper.addWarningsForUnvalidatedFiles(extractedContents, result, requiredDocuments);

        for (Classification c : fileClassification.values()) {
            if ("UNKNOWN".equals(c.type)) {
                result.getWarnings().add(c.fileName + ": Could not classify document (OCR text present but no strong match)");
            } else if (c.ambiguous) {
                result.getWarnings().add(c.fileName + ": Classification ambiguous, picked " + c.type + " (score=" + c.score + ")");
            }
        }

        validationHelper.setValidationMessage(result, requiredDocuments.size());

        return result;
    }
}

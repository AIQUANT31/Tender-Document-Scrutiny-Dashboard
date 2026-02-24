package com.example.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ValidationResult {
    
    private boolean valid;
    private List<String> matchedDocuments = new ArrayList<>();
    private List<String> missingDocuments = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> duplicateDocuments = new ArrayList<>();
    private String message;
    
    private Map<String, DocumentValidationDetail> documentDetails = new HashMap<>();

    public ValidationResult() {
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getMatchedDocuments() {
        return matchedDocuments;
    }

    public void setMatchedDocuments(List<String> matchedDocuments) {
        this.matchedDocuments = matchedDocuments;
    }

    public List<String> getMissingDocuments() {
        return missingDocuments;
    }

    public void setMissingDocuments(List<String> missingDocuments) {
        this.missingDocuments = missingDocuments;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getDuplicateDocuments() {
        return duplicateDocuments;
    }

    public void setDuplicateDocuments(List<String> duplicateDocuments) {
        this.duplicateDocuments = duplicateDocuments;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

   
    public boolean hasDuplicates() {
        return duplicateDocuments != null && !duplicateDocuments.isEmpty();
    }

    
    public int getValidatedCount() {
        return matchedDocuments != null ? matchedDocuments.size() : 0;
    }

    
    public int getMissingCount() {
        return missingDocuments != null ? missingDocuments.size() : 0;
    }

    
    public Map<String, DocumentValidationDetail> getDocumentDetails() {
        return documentDetails;
    }

    public void setDocumentDetails(Map<String, DocumentValidationDetail> documentDetails) {
        this.documentDetails = documentDetails;
    }

    public void addDocumentDetail(String filename, DocumentValidationDetail detail) {
        this.documentDetails.put(filename, detail);
    }
}

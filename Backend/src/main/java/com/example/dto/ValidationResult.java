package com.example.dto;

import java.util.ArrayList;
import java.util.List;


public class ValidationResult {
    
    private boolean valid;
    private List<String> matchedDocuments = new ArrayList<>();
    private List<String> missingDocuments = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> duplicateDocuments = new ArrayList<>();
    private String message;

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

    /**
     * Helper method to check if validation was successful
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    /**
     * Helper method to check if there are duplicate documents
     */
    public boolean hasDuplicates() {
        return duplicateDocuments != null && !duplicateDocuments.isEmpty();
    }

    /**
     * Helper method to get total count of validated documents
     */
    public int getValidatedCount() {
        return matchedDocuments != null ? matchedDocuments.size() : 0;
    }

    /**
     * Helper method to get total count of missing documents
     */
    public int getMissingCount() {
        return missingDocuments != null ? missingDocuments.size() : 0;
    }
}

package com.example.dto;

import java.util.List;

public class DocumentValidationResponse {
    
    private boolean valid;
    private List<String> matchedDocuments;
    private List<String> missingDocuments;
    private List<String> warnings;
    private List<String> duplicateDocuments;
    private String message;
    
    public DocumentValidationResponse() {}
    
    public DocumentValidationResponse(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
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
}

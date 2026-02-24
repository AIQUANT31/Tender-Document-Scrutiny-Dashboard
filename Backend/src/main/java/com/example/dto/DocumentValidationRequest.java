package com.example.dto;

import java.util.List;


public class DocumentValidationRequest {
    
    private List<String> requiredDocuments;
    private List<String> uploadedFileNames;
    
    public DocumentValidationRequest() {}
    
    public DocumentValidationRequest(List<String> requiredDocuments, List<String> uploadedFileNames) {
        this.requiredDocuments = requiredDocuments;
        this.uploadedFileNames = uploadedFileNames;
    }
    
    public List<String> getRequiredDocuments() {
        return requiredDocuments;
    }
    
    public void setRequiredDocuments(List<String> requiredDocuments) {
        this.requiredDocuments = requiredDocuments;
    }
    
    public List<String> getUploadedFileNames() {
        return uploadedFileNames;
    }
    
    public void setUploadedFileNames(List<String> uploadedFileNames) {
        this.uploadedFileNames = uploadedFileNames;
    }
}

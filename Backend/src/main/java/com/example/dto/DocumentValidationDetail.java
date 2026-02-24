package com.example.dto;

import java.util.ArrayList;
import java.util.List;


public class DocumentValidationDetail {
    
    private String documentType;
    private boolean valid;
    private String documentNumber;
    private double validationScore;
    private List<String> validatedFields = new ArrayList<>();
    private List<String> missingFields = new ArrayList<>();
    private String errorMessage;
    
    public DocumentValidationDetail() {
    }
    
    public DocumentValidationDetail(String documentType, boolean valid) {
        this.documentType = documentType;
        this.valid = valid;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public double getValidationScore() {
        return validationScore;
    }

    public void setValidationScore(double validationScore) {
        this.validationScore = validationScore;
    }

    public List<String> getValidatedFields() {
        return validatedFields;
    }

    public void setValidatedFields(List<String> validatedFields) {
        this.validatedFields = validatedFields;
    }

    public List<String> getMissingFields() {
        return missingFields;
    }

    public void setMissingFields(List<String> missingFields) {
        this.missingFields = missingFields;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String toString() {
        return "DocumentValidationDetail{" +
                "documentType='" + documentType + '\'' +
                ", valid=" + valid +
                ", documentNumber='" + documentNumber + '\'' +
                ", validationScore=" + validationScore +
                ", validatedFields=" + validatedFields +
                ", missingFields=" + missingFields +
                '}';
    }
}

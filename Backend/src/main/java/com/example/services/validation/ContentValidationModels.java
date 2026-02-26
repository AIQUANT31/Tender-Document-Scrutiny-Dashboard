package com.example.services.validation;

import java.util.List;

public class ContentValidationModels {

    public static class Classification {
        final String fileName;
        final String type;
        final int score;
        final String documentNumber;
        final boolean ambiguous;

        public Classification(String fileName, String type, int score, String documentNumber, boolean ambiguous) {
            this.fileName = fileName;
            this.type = type;
            this.score = score;
            this.documentNumber = documentNumber;
            this.ambiguous = ambiguous;
        }

        public String getFileName() {
            return fileName;
        }

        public String getType() {
            return type;
        }

        public int getScore() {
            return score;
        }

        public String getDocumentNumber() {
            return documentNumber;
        }

        public boolean isAmbiguous() {
            return ambiguous;
        }
    }

    public static class Score {
        final String type;
        final int score;
        final String documentNumber;

        public Score(String type, int score, String documentNumber) {
            this.type = type;
            this.score = score;
            this.documentNumber = documentNumber;
        }

        public String getType() {
            return type;
        }

        public int getScore() {
            return score;
        }

        public String getDocumentNumber() {
            return documentNumber;
        }
    }

    public static class DocumentMatchResult {
        private String fileName;
        private boolean matched;
        private String documentType;
        private String documentNumber;
        private double validationScore;
        private List<String> validatedFields;
        private List<String> missingFields;
        private String errorMessage;

        public DocumentMatchResult() {
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public boolean isMatched() {
            return matched;
        }

        public void setMatched(boolean matched) {
            this.matched = matched;
        }

        public String getDocumentType() {
            return documentType;
        }

        public void setDocumentType(String documentType) {
            this.documentType = documentType;
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
    }
}

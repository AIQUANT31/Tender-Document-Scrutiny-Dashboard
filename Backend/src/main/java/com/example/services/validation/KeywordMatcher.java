package com.example.services.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class KeywordMatcher {

    private static final Logger logger = LoggerFactory.getLogger(KeywordMatcher.class);

    
    private static final Pattern PAN_PATTERN = Pattern.compile(
            "\\b[A-Z]{5}[0-9]{4}[A-Z]\\b",
            Pattern.CASE_INSENSITIVE
    );

    
    private static final Pattern AADHAAR_PATTERN = Pattern.compile(
            "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"
    );

    
    private static final Pattern GSTIN_PATTERN = Pattern.compile(
            "\\b\\d{2}[A-Z]{5}\\d{4}[A-Z][A-Z0-9][A-Z]\\d\\b"
    );

    
    private static final Pattern CIN_PATTERN = Pattern.compile(
            "\\b[LU]\\d{5}[A-Z]{2}\\d{4}[A-Z]{3}\\d{6}\\b",
            Pattern.CASE_INSENSITIVE
    );

    
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}\\b"
    );
    
    
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+\\b"
    );

    
    private static final Map<String, List<String>> DOCUMENT_KEYWORDS = new HashMap<>();

    
    private static final Map<String, DocumentTemplate> DOCUMENT_TEMPLATES = new HashMap<>();

    static {
        
        DOCUMENT_KEYWORDS.put("PAN", Arrays.asList(
                "pan", "pan card", "permanent account number",
                "permanentaccountnumber", "income tax department",
                "panno", "pan no", "pan number"
        ));

        DOCUMENT_KEYWORDS.put("AADHAAR", Arrays.asList(
                "aadhaar", "aadhar", "uidai", "uid number",
                "unique identification", "aadhar card", "aadhaar number"
        ));

        DOCUMENT_KEYWORDS.put("GST", Arrays.asList(
                "gst", "gstin", "gst registration", "goods and services tax",
                "gstn", "gst certificate", "gst number"
        ));

        DOCUMENT_KEYWORDS.put("INCOME_TAX", Arrays.asList(
                "income tax", "income tax clearance", "tax clearance", "itr",
                "income tax return", "itr filing", "tax assessment"
        ));

        DOCUMENT_KEYWORDS.put("EXPERIENCE", Arrays.asList(
                "experience certificate", "work experience", "experience letter",
                "employment certificate", "service certificate", "work history"
        ));

        DOCUMENT_KEYWORDS.put("COMPANY_REG", Arrays.asList(
                "company registration", "certificate of incorporation", "roc",
                "incorporation certificate", "company incorporation",
                "ministry of corporate affairs", "mca",
                "corporate identification number", "cin",
                "memorandum of association", "articles of association",
                "moa", "aoa"
        ));

        DOCUMENT_KEYWORDS.put("INSURANCE", Arrays.asList(
                "insurance", "insurance certificate", "insurance policy",
                "insurance cover", "policy document", "coverage certificate"
        ));

        
        initDocumentTemplates();
    }

    
    private static void initDocumentTemplates() {
        
        DocumentTemplate panTemplate = new DocumentTemplate("PAN");
        panTemplate.addRequiredField("documentNumber", Arrays.asList(
                "pan", "permanent account number", "panno", "pan no"
        ));
        panTemplate.addRequiredField("name", Arrays.asList(
                "name", "name of", "holder name", "card holder"
        ));
        panTemplate.addRequiredField("fatherName", Arrays.asList(
                "father", "father's name", "father name", "parent"
        ));
        panTemplate.addRequiredField("dateOfBirth", Arrays.asList(
                "dob", "date of birth", "birth date", "birthday"
        ));
        panTemplate.addRequiredField("signature", Arrays.asList(
                "signature", "signed", "sign"
        ));
        panTemplate.setNumberPattern("[A-Z]{5}[0-9]{4}[A-Z]");
        DOCUMENT_TEMPLATES.put("PAN", panTemplate);

        
        DocumentTemplate aadhaarTemplate = new DocumentTemplate("AADHAAR");
        aadhaarTemplate.addRequiredField("documentNumber", Arrays.asList(
                "aadhaar", "uid", "unique identification", "aadhar number"
        ));
        aadhaarTemplate.addRequiredField("name", Arrays.asList(
                "name", "name of", "holder name"
        ));
        aadhaarTemplate.addRequiredField("dateOfBirth", Arrays.asList(
                "dob", "date of birth", "birth date", "birthday", "year of birth"
        ));
        aadhaarTemplate.addRequiredField("gender", Arrays.asList(
                "gender", "sex", "male", "female"
        ));
        aadhaarTemplate.addRequiredField("address", Arrays.asList(
                "address", "residence", "village", "city", "district", "state"
        ));
        aadhaarTemplate.setNumberPattern("\\d{12}");
        DOCUMENT_TEMPLATES.put("AADHAAR", aadhaarTemplate);

        
        DocumentTemplate gstTemplate = new DocumentTemplate("GST");
        gstTemplate.addRequiredField("documentNumber", Arrays.asList(
                "gstin", "gst registration", "gst number", "registration number"
        ));
        gstTemplate.addRequiredField("businessName", Arrays.asList(
                "legal name", "business name", "trade name", "company name", "firm name"
        ));
        gstTemplate.addRequiredField("address", Arrays.asList(
                "address", "principal place", "business address", "registered address"
        ));
        gstTemplate.addRequiredField("state", Arrays.asList(
                "state", "state code", "state name"
        ));
        gstTemplate.setNumberPattern("\\d{2}[A-Z]{5}\\d{4}[A-Z][A-Z0-9][A-Z]\\d");
        DOCUMENT_TEMPLATES.put("GST", gstTemplate);

        
        DocumentTemplate itrTemplate = new DocumentTemplate("INCOME_TAX");
        itrTemplate.addRequiredField("documentType", Arrays.asList(
                "itr", "income tax return", "return of income", "tax return"
        ));
        itrTemplate.addRequiredField("assessmentYear", Arrays.asList(
                "assessment year", "ay", "financial year", "fy"
        ));
        itrTemplate.addRequiredField("name", Arrays.asList(
                "name", "assessee name", "taxpayer name"
        ));
        itrTemplate.addRequiredField("income", Arrays.asList(
                "income", "total income", "gross income", "taxable income"
        ));
        itrTemplate.addRequiredField("tax", Arrays.asList(
                "tax", "tax payable", "tax deducted", "tds"
        ));
        DOCUMENT_TEMPLATES.put("INCOME_TAX", itrTemplate);

        
        DocumentTemplate expTemplate = new DocumentTemplate("EXPERIENCE");
        expTemplate.addRequiredField("employeeName", Arrays.asList(
                "employee name", "name of employee", "candidate name",
                "this is to certify", "to certify that", "certify that",
                "mr.", "mrs.", "ms.", "shri", "smt"
        ));
        expTemplate.addRequiredField("companyName", Arrays.asList(
                "company name", "organization", "employer", "institution", "company"
        ));
        expTemplate.addRequiredField("designation", Arrays.asList(
                "designation", "position", "job title", "role", "post",
                "worked as", "employed as"
        ));
        expTemplate.addRequiredField("duration", Arrays.asList(
                "duration", "period", "from date", "to date", "working period",
                "years", "months", "joining date", "relieving date",
                "from", "to", "since", "till", "tenure"
        ));
        expTemplate.addRequiredField("certificateType", Arrays.asList(
                "experience", "experience certificate", "work experience", "service certificate",
                "employment certificate", "letter of experience", "experience letter"
        ));
        DOCUMENT_TEMPLATES.put("EXPERIENCE", expTemplate);

        
        DocumentTemplate companyTemplate = new DocumentTemplate("COMPANY_REG");
        companyTemplate.addRequiredField("companyName", Arrays.asList(
                "company name", "name of company", "corporate name"
        ));
        companyTemplate.addRequiredField("registrationNumber", Arrays.asList(
                "cin", "company identification number", "registration number",
                "incorporation number", "roc"
        ));
        companyTemplate.addRequiredField("dateOfIncorporation", Arrays.asList(
                "date of incorporation", "incorporated on", "incorporation date",
                "formation date"
        ));
        companyTemplate.addRequiredField("registeredAddress", Arrays.asList(
                "registered office", "registered address", "principal place"
        ));
        companyTemplate.addRequiredField("capital", Arrays.asList(
                "authorized capital", "paid up capital", "share capital"
        ));
        DOCUMENT_TEMPLATES.put("COMPANY_REG", companyTemplate);

        
        DocumentTemplate insuranceTemplate = new DocumentTemplate("INSURANCE");
        insuranceTemplate.addRequiredField("policyNumber", Arrays.asList(
                "policy number", "policy no", "policy id", "certificate number"
        ));
        insuranceTemplate.addRequiredField("insuredName", Arrays.asList(
                "insured name", "proposer name", "policy holder", "beneficiary"
        ));
        insuranceTemplate.addRequiredField("insuranceCompany", Arrays.asList(
                "insurance company", "insurer", "company name", "issued by"
        ));
        insuranceTemplate.addRequiredField("validity", Arrays.asList(
                "validity", "valid from", "valid until", "expiry date", "policy period",
                "start date", "end date", "coverage"
        ));
        insuranceTemplate.addRequiredField("sumInsured", Arrays.asList(
                "sum insured", "coverage amount", "sum assured", "limit"
        ));
        DOCUMENT_TEMPLATES.put("INSURANCE", insuranceTemplate);
    }

    
    public List<String> getKeywordsForDocument(String documentName) {
        if (documentName == null) return new ArrayList<>();

        String docLower = documentName.toLowerCase();

        
        if (containsWord(docLower, "pan")) return DOCUMENT_KEYWORDS.get("PAN");
        if (docLower.contains("aadhaar") || docLower.contains("aadhar") || docLower.contains("uid")) return DOCUMENT_KEYWORDS.get("AADHAAR");
        if (docLower.contains("gst") || docLower.contains("gstin")) return DOCUMENT_KEYWORDS.get("GST");
        if (docLower.contains("income tax") || docLower.contains("itr") || docLower.contains("tax")) return DOCUMENT_KEYWORDS.get("INCOME_TAX");
        if (docLower.contains("experience")) return DOCUMENT_KEYWORDS.get("EXPERIENCE");
        if (docLower.contains("company") || docLower.contains("incorporation") || docLower.contains("roc")) return DOCUMENT_KEYWORDS.get("COMPANY_REG");
        if (docLower.contains("insurance")) return DOCUMENT_KEYWORDS.get("INSURANCE");

        return new ArrayList<>();
    }

    
    public DocumentTemplate getDocumentTemplate(String documentName) {
        if (documentName == null) return null;
        
        String docLower = documentName.toLowerCase();
        
        if (containsWord(docLower, "pan")) return DOCUMENT_TEMPLATES.get("PAN");
        if (docLower.contains("aadhaar") || docLower.contains("aadhar") || docLower.contains("uid")) return DOCUMENT_TEMPLATES.get("AADHAAR");
        if (docLower.contains("gst") || docLower.contains("gstin")) return DOCUMENT_TEMPLATES.get("GST");
        if (docLower.contains("income tax") || docLower.contains("itr") || docLower.contains("tax")) return DOCUMENT_TEMPLATES.get("INCOME_TAX");
        if (docLower.contains("experience")) return DOCUMENT_TEMPLATES.get("EXPERIENCE");
        if (docLower.contains("company") || docLower.contains("incorporation") || docLower.contains("roc")) return DOCUMENT_TEMPLATES.get("COMPANY_REG");
        if (docLower.contains("insurance")) return DOCUMENT_TEMPLATES.get("INSURANCE");
        
        return null;
    }

    private boolean containsWord(String text, String word) {
        if (text == null || word == null || word.isBlank()) return false;
        return Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE)
                .matcher(text)
                .find();
    }

    
    public DocumentValidationResult validateDocumentComprehensively(String documentType, String content) {
        DocumentValidationResult result = new DocumentValidationResult();
        result.setValid(false);
        
        if (content == null || content.isBlank()) {
            result.setErrorMessage("No content to validate");
            return result;
        }

        String contentLower = content.toLowerCase();
        
        
        DocumentTemplate template = getDocumentTemplate(documentType);
        if (template == null) {
            
            return basicValidation(documentType, content);
        }
        
        result.setDocumentType(template.getDocumentType());
        
        
        if (template.getNumberPattern() != null) {
            Pattern pattern = Pattern.compile(template.getNumberPattern(), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                result.setDocumentNumber(matcher.group());
                result.getValidatedFields().add("documentNumber");
                logger.info("Document number validated: {}", result.getDocumentNumber());
            }
        }
        
        
        int fieldsFound = 0;
        int totalFields = template.getRequiredFields().size();
        
        for (Map.Entry<String, List<String>> fieldEntry : template.getRequiredFields().entrySet()) {
            String fieldName = fieldEntry.getKey();
            List<String> fieldKeywords = fieldEntry.getValue();
            
            
            if (fieldName.equals("documentNumber")) continue;
            
            boolean fieldFound = false;
            for (String keyword : fieldKeywords) {
                if (contentLower.contains(keyword.toLowerCase())) {
                    fieldFound = true;
                    break;
                }
            }
            
            if (fieldFound) {
                result.getValidatedFields().add(fieldName);
                fieldsFound++;
                logger.debug("Field '{}' found in document", fieldName);
            } else {
                result.getMissingFields().add(fieldName);
                logger.debug("Field '{}' NOT found in document", fieldName);
            }
        }
        
        
        int requiredFieldsWithNumber = totalFields + (template.getNumberPattern() != null ? 1 : 0);
        int fieldsValidatedWithNumber = fieldsFound + (result.getDocumentNumber() != null ? 1 : 0);
        
        double score = (double) fieldsValidatedWithNumber / requiredFieldsWithNumber * 100;
        result.setValidationScore(score);
        
        
        if (score >= 60 || (result.getDocumentNumber() != null && fieldsFound >= 1)) {
            result.setValid(true);
            result.setMessage("Document validated successfully");
        } else {
            result.setValid(false);
            result.setErrorMessage("Document does not contain expected content. Missing: " + 
                    String.join(", ", result.getMissingFields()));
        }
        
        return result;
    }

    
    private DocumentValidationResult basicValidation(String documentType, String content) {
        DocumentValidationResult result = new DocumentValidationResult();
        result.setDocumentType(documentType);
        
        List<String> keywords = getKeywordsForDocument(documentType);
        
        if (keywords.isEmpty()) {
            result.setValid(content.length() > 10); 
            return result;
        }
        
        int matches = 0;
        String contentLower = content.toLowerCase();
        
        for (String keyword : keywords) {
            if (contentLower.contains(keyword.toLowerCase())) {
                matches++;
            }
        }
        
        result.setValid(matches >= 2);
        result.setValidationScore(matches * 100.0 / keywords.size());
        
        return result;
    }

    public String validatePanCardInContent(String content) {

    if (content == null || content.isBlank())
        return null;

    logger.info("Validating PAN in content of length: {}", content.length());

    String contentLower = content.toLowerCase();

    
    
    List<String> strongPanContext = Arrays.asList(
            "income tax department",
            "income tax",
            "permanent account number",
            "pan card"
    );

    
    String normalized = content
            .replaceAll("[^A-Za-z0-9]", "")   
            .toUpperCase();

    logger.debug("Normalized content: {}", normalized.substring(0, Math.min(100, normalized.length())));

    
    Pattern panPattern = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]");
    Matcher matcher = panPattern.matcher(normalized);

    if (matcher.find()) {
        String pan = matcher.group();
        
        boolean hasStrongContext = false;
        for (String ctx : strongPanContext) {
            if (contentLower.contains(ctx)) {
                hasStrongContext = true;
                break;
            }
        }

        int keywordHits = 0;
        List<String> panKeywords = DOCUMENT_KEYWORDS.get("PAN");
        if (panKeywords != null) {
            for (String kw : panKeywords) {
                if (kw != null && !kw.isBlank() && contentLower.contains(kw.toLowerCase())) {
                    keywordHits++;
                }
            }
        }

        
        if (hasStrongContext || keywordHits >= 2) {
            logger.info("Valid PAN detected with context (hits={}, strongContext={}): {}", keywordHits, hasStrongContext, pan);
            return pan;
        }

        logger.warn("PAN-like pattern found but missing context (hits={}, strongContext={})", keywordHits, hasStrongContext);
        return null;
    }

    
    Pattern panPatternCaseInsensitive = Pattern.compile("\\b[A-Za-z]{5}[0-9]{4}[A-Za-z]\\b");
    Matcher matcher2 = panPatternCaseInsensitive.matcher(content);
    if (matcher2.find()) {
        String pan = matcher2.group().toUpperCase();
        boolean hasStrongContext = false;
        for (String ctx : strongPanContext) {
            if (contentLower.contains(ctx)) {
                hasStrongContext = true;
                break;
            }
        }

        int keywordHits = 0;
        List<String> panKeywords = DOCUMENT_KEYWORDS.get("PAN");
        if (panKeywords != null) {
            for (String kw : panKeywords) {
                if (kw != null && !kw.isBlank() && contentLower.contains(kw.toLowerCase())) {
                    keywordHits++;
                }
            }
        }

        if (hasStrongContext || keywordHits >= 2) {
            logger.info("Valid PAN detected with context (hits={}, strongContext={}): {}", keywordHits, hasStrongContext, pan);
            return pan;
        }

        logger.warn("PAN-like token found but missing context (hits={}, strongContext={})", keywordHits, hasStrongContext);
        return null;
    }

    
    
    int keywordHits = 0;
    List<String> panKeywords = DOCUMENT_KEYWORDS.get("PAN");
    if (panKeywords != null) {
        for (String kw : panKeywords) {
            if (kw != null && !kw.isBlank() && contentLower.contains(kw.toLowerCase())) {
                keywordHits++;
            }
        }
    }
    boolean hasStrongContext = false;
    for (String ctx : strongPanContext) {
        if (contentLower.contains(ctx)) {
            hasStrongContext = true;
            break;
        }
    }
    if (hasStrongContext && keywordHits >= 2) {
        logger.info("⚠ PAN keywords detected with strong context but PAN number unclear (hits={})", keywordHits);
        return "PAN_KEYWORD_FOUND";
    }

    logger.warn(" PAN not detected");
    return null;
}

    public String validateAadharCardInContent(String content) {
        if (content == null) return null;

        String contentLower = content.toLowerCase();

        
        
        List<String> strongAadhaarContext = Arrays.asList("uidai", "aadhaar", "aadhar", "unique identification");

        boolean hasContext = false;
        for (String ctx : strongAadhaarContext) {
            if (contentLower.contains(ctx)) {
                hasContext = true;
                break;
            }
        }

        
        int keywordHits = 0;
        List<String> aadhaarKeywords = DOCUMENT_KEYWORDS.get("AADHAAR");
        if (aadhaarKeywords != null) {
            for (String kw : aadhaarKeywords) {
                if (kw != null && !kw.isBlank() && contentLower.contains(kw.toLowerCase())) {
                    keywordHits++;
                }
            }
        }

        Matcher matcher = AADHAAR_PATTERN.matcher(content);
        if (matcher.find()) {
            String aadhaar = matcher.group().replaceAll("[\\s-]", "");

            
            if (hasContext || keywordHits >= 2) {
                logger.info("Valid Aadhaar detected with context (hits={}, strongContext={}): {}", keywordHits, hasContext, aadhaar);
                return aadhaar;
            }

            logger.warn("Aadhaar-like 12-digit number found but missing context (hits={}, strongContext={})", keywordHits, hasContext);
            return null;
        }

        
        if (hasContext && keywordHits >= 2) {
            logger.info("⚠ Aadhaar keywords detected with strong context but number unclear (hits={})", keywordHits);
            return "AADHAAR_KEYWORD_FOUND";
        }

        return null;
    }

    public String validateGSTInContent(String content) {
        if (content == null) return null;

        Matcher matcher = GSTIN_PATTERN.matcher(content);
        if (matcher.find()) {
            String gstin = matcher.group();
            logger.info("Valid GSTIN detected: {}", gstin);
            return gstin;
        }

        return containsKeyword(content, DOCUMENT_KEYWORDS.get("GST"))
                ? "GST_KEYWORD_FOUND"
                : null;
    }

    public String validateIncomeTaxInContent(String content) {
        if (content == null) return null;

        return containsKeyword(content, DOCUMENT_KEYWORDS.get("INCOME_TAX"))
                ? "INCOME_TAX_FOUND"
                : null;
    }

    public String validateExperienceCertificateInContent(String content) {
        if (content == null || content.isBlank()) return null;

        
        if (containsKeyword(content, DOCUMENT_KEYWORDS.get("EXPERIENCE"))) {
            return "EXPERIENCE_FOUND";
        }

        
        String lower = content.toLowerCase();

        int evidence = 0;
        boolean hasDate = DATE_PATTERN.matcher(content).find();

        if (lower.contains("experience")) evidence += 2;
        if (lower.contains("worked as") || lower.contains("has worked") || lower.contains("worked with")
                || lower.contains("employed as") || lower.contains("employment")) evidence += 1;
        if (lower.contains("designation") || lower.contains("position") || lower.contains("job title")
                || lower.contains("role")) evidence += 1;
        if ((lower.contains("from") && lower.contains("to")) || lower.contains("joining")
                || lower.contains("relieving") || lower.contains("tenure") || lower.contains("period")) evidence += 1;
        if (hasDate) evidence += 1;

        
        if (evidence >= 3 && (hasDate || lower.contains("experience"))) {
            return "EXPERIENCE_FOUND";
        }

        return null;
    }

    public String validateCompanyRegistrationInContent(String content) {
        if (content == null) return null;

        
        Matcher cinMatcher = CIN_PATTERN.matcher(content);
        if (cinMatcher.find()) {
            String cin = cinMatcher.group().toUpperCase();
            logger.info("Valid CIN detected: {}", cin);
            return cin;
        }

        return containsKeyword(content, DOCUMENT_KEYWORDS.get("COMPANY_REG"))
                ? "COMPANY_REG_FOUND"
                : null;
    }

    public String validateInsuranceInContent(String content) {
        if (content == null) return null;

        return containsKeyword(content, DOCUMENT_KEYWORDS.get("INSURANCE"))
                ? "INSURANCE_FOUND"
                : null;
    }

    
    public boolean checkContentMatch(String requiredDoc, List<String> keywords, String fileName, String content) {
        if (content == null || content.isEmpty()) return false;
        
        
        if (keywords == null || keywords.isEmpty()) {
            return content.toLowerCase().contains(requiredDoc.toLowerCase());
        }
        
        String contentLower = content.toLowerCase();

        
        
        if (requiredDoc != null && !requiredDoc.isBlank() && contentLower.contains(requiredDoc.toLowerCase())) {
            return true;
        }

        int matches = 0;
        for (String keyword : keywords) {
            if (keyword == null) continue;
            String kw = keyword.toLowerCase().trim();
            if (kw.isEmpty()) continue;
            if (contentLower.contains(kw)) {
                matches++;
            }
        }

        
        
        
        int threshold = (keywords.size() >= 6) ? 2 : 1;
        return matches >= threshold;
    }

    private boolean containsKeyword(String content, List<String> keywords) {
        if (content == null || keywords == null) return false;

        String lower = content.toLowerCase();

        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase())) {
                logger.info("Keyword '{}' detected", kw);
                return true;
            }
        }
        return false;
    }

    
    public static class DocumentTemplate {
        private String documentType;
        private Map<String, List<String>> requiredFields = new HashMap<>();
        private String numberPattern;

        public DocumentTemplate(String documentType) {
            this.documentType = documentType;
        }

        public void addRequiredField(String fieldName, List<String> keywords) {
            requiredFields.put(fieldName, keywords);
        }

        public String getDocumentType() { return documentType; }
        public Map<String, List<String>> getRequiredFields() { return requiredFields; }
        public String getNumberPattern() { return numberPattern; }
        public void setNumberPattern(String pattern) { this.numberPattern = pattern; }
    }

    
    public static class DocumentValidationResult {
        private boolean valid;
        private String documentType;
        private String documentNumber;
        private String message;
        private String errorMessage;
        private double validationScore;
        private List<String> validatedFields = new ArrayList<>();
        private List<String> missingFields = new ArrayList<>();

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        public String getDocumentNumber() { return documentNumber; }
        public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public double getValidationScore() { return validationScore; }
        public void setValidationScore(double validationScore) { this.validationScore = validationScore; }
        public List<String> getValidatedFields() { return validatedFields; }
        public List<String> getMissingFields() { return missingFields; }

        @Override
        public String toString() {
            return "DocumentValidationResult{" +
                    "valid=" + valid +
                    ", documentType='" + documentType + '\'' +
                    ", documentNumber='" + documentNumber + '\'' +
                    ", validationScore=" + validationScore +
                    ", validatedFields=" + validatedFields +
                    ", missingFields=" + missingFields +
                    '}';
        }
    }
}

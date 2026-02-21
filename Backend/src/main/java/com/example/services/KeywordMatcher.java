package com.example.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class KeywordMatcher {

    private static final Logger logger = LoggerFactory.getLogger(KeywordMatcher.class);
    
    private static final Pattern PAN_CARD_PATTERN = Pattern.compile(
        "\\b[A-Z]{5}[0-9]{4}[A-Z]\\b"
    );
    
    private static final Pattern AADHAR_CARD_PATTERN = Pattern.compile(
        "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"
    );

    private static final Map<String, List<String>> DOCUMENT_KEYWORDS = new HashMap<>();

    static {
        DOCUMENT_KEYWORDS.put("AADHAR", Arrays.asList("aadhar", "aadhaar", "uidai", "uid", "aadhar card", "aadhaar card", "unique identification", "uid number"));
        DOCUMENT_KEYWORDS.put("PAN", Arrays.asList("pan", "permanent account", "income tax", "pan card", "pan number"));
        DOCUMENT_KEYWORDS.put("GST", Arrays.asList("gst", "goods and services", "gstin", "tax registration"));
        DOCUMENT_KEYWORDS.put("TENDER", Arrays.asList("tender", "bid document", "proposal"));
        DOCUMENT_KEYWORDS.put("COMPANY_REGISTRATION", Arrays.asList("company registration", "moa", "aoa", "incorporation"));
        DOCUMENT_KEYWORDS.put("POWER_OF_ATTORNEY", Arrays.asList("power of attorney", "poa", "authorization"));
        DOCUMENT_KEYWORDS.put("BANK_STATEMENT", Arrays.asList("bank statement", "bank account"));
        DOCUMENT_KEYWORDS.put("AUDITED_FINANCIALS", Arrays.asList("audited", "financial statements", "balance sheet"));
        DOCUMENT_KEYWORDS.put("WORK_EXPERIENCE", Arrays.asList("work experience", "completed projects"));
        DOCUMENT_KEYWORDS.put("TECHNICAL_CAPABILITY", Arrays.asList("technical", "capability statement"));
        DOCUMENT_KEYWORDS.put("QUALITY_CERTIFICATE", Arrays.asList("quality", "iso", "certification"));
        DOCUMENT_KEYWORDS.put("EMD", Arrays.asList("emd", "earnest money", "security deposit", "bid security"));
        DOCUMENT_KEYWORDS.put("TENDER_FEE", Arrays.asList("tender fee", "document fee"));
        DOCUMENT_KEYWORDS.put("AFFIDAVIT", Arrays.asList("affidavit", "declaration", "undertaking"));
        DOCUMENT_KEYWORDS.put("NATIONALITY", Arrays.asList("nationality", "citizenship"));
        DOCUMENT_KEYWORDS.put("REGISTRATION", Arrays.asList("registration", "license", "permit"));
    }
    public List<String> getKeywordsForDocumentType(String documentType) {
        return DOCUMENT_KEYWORDS.getOrDefault(documentType.toUpperCase(), new ArrayList<>());
    }

    public List<String> getSupportedDocumentTypes() {
        return new ArrayList<>(DOCUMENT_KEYWORDS.keySet());
    }

    private List<String> getKeywordsInternal(String documentType) {
        String docTypeLower = documentType.toLowerCase().trim();
        
        for (Map.Entry<String, List<String>> entry : DOCUMENT_KEYWORDS.entrySet()) {
            if (entry.getKey().toLowerCase().equals(docTypeLower)) {
                logger.debug("Found exact match for '{}' in DOCUMENT_KEYWORDS", documentType);
                return entry.getValue();
            }
        }
        
       
        for (Map.Entry<String, List<String>> entry : DOCUMENT_KEYWORDS.entrySet()) {
            String key = entry.getKey().toLowerCase();
            
            if (docTypeLower.contains(key) || key.contains(docTypeLower) ||
                (key.equals("pan") && (docTypeLower.contains("pan") || docTypeLower.contains("pan card"))) ||
                (key.equals("aadhar") && (docTypeLower.contains("aadhar") || docTypeLower.contains("aadhaar"))) ||
                (key.equals("gst") && docTypeLower.contains("gst"))) {
                logger.debug("Found partial match: '{}' matches key '{}'", documentType, entry.getKey());
                return entry.getValue();
            }
        }
        
        // Special case: check for common document names
        if (docTypeLower.contains("pan") || docTypeLower.contains("income tax")) {
            logger.debug("Special case: '{}' matched to PAN keywords", documentType);
            return DOCUMENT_KEYWORDS.get("PAN");
        }
        
        if (docTypeLower.contains("aadhar") || docTypeLower.contains("aadhaar") || docTypeLower.contains("uid")) {
            logger.debug("Special case: '{}' matched to AADHAR keywords", documentType);
            return DOCUMENT_KEYWORDS.get("AADHAR");
        }
        
        if (docTypeLower.contains("gst")) {
            logger.debug("Special case: '{}' matched to GST keywords", documentType);
            return DOCUMENT_KEYWORDS.get("GST");
        }
        
        logger.debug("No keywords found for '{}'", documentType);
        return new ArrayList<>();
    }

    public List<String> getKeywordsForDocument(String documentName) {
        List<String> keywords = new ArrayList<>();
        String docNameLower = documentName.toLowerCase().trim();
        
        // First try to get predefined keywords from DOCUMENT_KEYWORDS map
        List<String> predefined = getKeywordsInternal(docNameLower);
        if (predefined != null && !predefined.isEmpty()) {
            keywords.addAll(predefined);
            logger.debug("Added {} predefined keywords for '{}': {}", predefined.size(), documentName, predefined);
        }
        
        // ALWAYS add the full document name as a keyword (critical for matching)
        if (!docNameLower.isEmpty() && !keywords.contains(docNameLower)) {
            keywords.add(docNameLower);
        }
        
        // Add individual words from the document name (for flexible matching)
        String[] words = docNameLower.split("[\\s,_-]+");
        for (String word : words) {
            word = word.trim();
            if (word.length() >= 2 && !keywords.contains(word)) {
                keywords.add(word);
            }
        }
        
        // Add keywords from DOCUMENT_KEYWORDS that match
        for (Map.Entry<String, List<String>> entry : DOCUMENT_KEYWORDS.entrySet()) {
            String docType = entry.getKey().toLowerCase();
            if (docNameLower.contains(docType) || docType.contains(docNameLower)) {
                List<String> typeKeywords = entry.getValue();
                for (String typeKw : typeKeywords) {
                    if (!keywords.contains(typeKw.toLowerCase())) {
                        keywords.add(typeKw.toLowerCase());
                    }
                }
            }
        }
        
        // Remove duplicates while preserving order
        List<String> uniqueKeywords = new ArrayList<>();
        for (String kw : keywords) {
            String kwLower = kw.toLowerCase().trim();
            if (!kwLower.isEmpty() && !uniqueKeywords.contains(kwLower)) {
                uniqueKeywords.add(kwLower);
            }
        }
        
        logger.info("Final keywords for '{}': {}", documentName, uniqueKeywords);
        return uniqueKeywords;
    }

  
    public boolean checkContentMatch(String requiredDoc, List<String> keywords, String fileName, String content) {
        if (matchesRequiredDocument(requiredDoc, fileName)) return true;
        if (content == null || content.isEmpty()) return false;
        
        int matches = 0;
        for (String keyword : keywords) {
            if (content.contains(keyword.toLowerCase())) matches++;
        }
        
        return matches >= 2;
    }

  
    public boolean matchesRequiredDocument(String requiredDoc, String fileName) {
        if (fileName.contains(requiredDoc)) return true;
        
        for (Map.Entry<String, List<String>> entry : DOCUMENT_KEYWORDS.entrySet()) {
            String docType = entry.getKey().toLowerCase();
            List<String> keywords = entry.getValue();
            
            if (requiredDoc.contains(docType) || docType.contains(requiredDoc)) {
                for (String kw : keywords) {
                    if (fileName.contains(kw)) return true;
                }
            }
            
            for (String kw : keywords) {
                if (requiredDoc.contains(kw) && fileName.contains(kw)) return true;
            }
        }
        
        String[] words = requiredDoc.split("[\\s,_-]+");
        for (String word : words) {
            if (word.length() > 3 && fileName.contains(word)) return true;
        }
        
        return false;
    }

    public String validatePanCardInContent(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        
       
        Matcher matcher = PAN_CARD_PATTERN.matcher(content);
        if (matcher.find()) {
            String panNumber = matcher.group();
            logger.info("Found valid PAN card number: {}", panNumber);
            return panNumber;
        }
        
     
        String[] panPatterns = {
            "pan\\s*[:|=]\\s*([A-Z]{5}[0-9]{4}[A-Z])",
            "panno\\s*[:|=]\\s*([A-Z]{5}[0-9]{4}[A-Z])",
            "pan\\s*number\\s*[:|=]\\s*([A-Z]{5}[0-9]{4}[A-Z])",
            "permanent\\s*account\\s*number\\s*[:|=]\\s*([A-Z]{5}[0-9]{4}[A-Z])"
        };
        
        for (String pattern : panPatterns) {
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(content);
            if (m.find()) {
                String panNumber = m.group(1);
                logger.info("Found PAN card number with pattern: {}", panNumber);
                return panNumber;
            }
        }
        
   
        String contentLower = content.toLowerCase();
        boolean hasPanKeyword = false;
        List<String> panKeywords = DOCUMENT_KEYWORDS.get("PAN");
        for (String keyword : panKeywords) {
            if (contentLower.contains(keyword)) {
                hasPanKeyword = true;
                logger.info("Found PAN keyword: {}", keyword);
                break;
            }
        }
        
        if (hasPanKeyword) {
            // Keywords found - try one more time with more flexible pattern
            Pattern flexiblePan = Pattern.compile("\\b[A-Za-z]{5}[0-9]{4}[A-Za-z]\\b");
            Matcher flexMatcher = flexiblePan.matcher(content);
            if (flexMatcher.find()) {
                String panNumber = flexMatcher.group();
                logger.info("Found potential PAN card number (flexible): {}", panNumber);
                return panNumber.toUpperCase();
            }
            
            logger.info("PAN keywords found but no valid PAN number format detected");
            return "PAN_KEYWORD_FOUND";
        }
        
        logger.debug("No PAN keywords or PAN number found in content");
        return null;
    }

   
    public String validateAadharCardInContent(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        
     
        Matcher matcher = AADHAR_CARD_PATTERN.matcher(content);
        if (matcher.find()) {
            String aadharNumber = matcher.group();
            logger.info("Found valid Aadhaar card number: {}", aadharNumber);
            return aadharNumber;
        }
        
        
        String cleanContent = content.replaceAll("[\\s-]", "");
        if (cleanContent.length() >= 12) {
            Pattern twelveDigits = Pattern.compile("\\b\\d{12}\\b");
            Matcher m = twelveDigits.matcher(cleanContent);
            if (m.find()) {
                String aadharNumber = m.group();
                logger.info("Found Aadhaar card number (cleaned): {}", aadharNumber);
                return aadharNumber;
            }
        }
        
        String contentLower = content.toLowerCase();
        List<String> aadharKeywords = DOCUMENT_KEYWORDS.get("AADHAR");
        for (String keyword : aadharKeywords) {
            if (contentLower.contains(keyword)) {
                logger.info("Found Aadhaar keyword: {}", keyword);
                return "AADHAR_KEYWORD_FOUND";
            }
        }
        
        logger.debug("No Aadhaar keywords or Aadhaar number found in content");
        return null;
    }

   
    public double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.length() == 0 || s2.length() == 0) return 0.0;

        Set<String> set1 = new HashSet<>(Arrays.asList(s1.split("[\\s,_-]+")));
        Set<String> set2 = new HashSet<>(Arrays.asList(s2.split("[\\s,_-]+")));

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }
}

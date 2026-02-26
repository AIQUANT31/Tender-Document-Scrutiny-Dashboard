package com.example.services.document;

import com.example.services.validation.ContentValidationModels.Classification;
import com.example.services.validation.ContentValidationModels.Score;
import com.example.services.validation.KeywordMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DocumentClassifier {

    private static final Logger logger = LoggerFactory.getLogger(DocumentClassifier.class);

    @Autowired
    private KeywordMatcher keywordMatcher;

    public Map<String, Classification> classifyExtractedContents(Map<String, String> extractedContents) {
        logger.info("Starting classification of {} documents", extractedContents.size());
        Map<String, Classification> out = new HashMap<>();

        for (Map.Entry<String, String> entry : extractedContents.entrySet()) {
            String fileName = entry.getKey();
            String content = entry.getValue();

            if (content == null || content.startsWith("IMAGE_PDF_FALLBACK") || content.isBlank()) {
                logger.debug("Skipping file {} - empty or image fallback content", fileName);
                out.put(fileName, new Classification(fileName, "UNKNOWN", 0, null, false));
                continue;
            }

            Score pan = scorePan(content);
            Score aadhaar = scoreAadhaar(content);
            Score gst = scoreGst(content);
            Score incomeTax = scoreIncomeTax(content);
            Score experience = scoreExperience(content);
            Score companyReg = scoreCompanyReg(content);
            Score insurance = scoreInsurance(content);

            List<Score> scores = Arrays.asList(pan, aadhaar, gst, incomeTax, experience, companyReg, insurance);
            scores.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

            Score best = scores.get(0);
            Score second = scores.get(1);

            if (best.getScore() < 50) {
                logger.debug("Classified file {} as UNKNOWN with score {}", fileName, best.getScore());
                out.put(fileName, new Classification(fileName, "UNKNOWN", best.getScore(), best.getDocumentNumber(), false));
                continue;
            }

            boolean ambiguous = second.getScore() >= 50 && (best.getScore() - second.getScore()) <= 10;
            
            if (ambiguous) {
                logger.warn("Classification ambiguous for file {}: best={} (score={}), second={} (score={})",
                    fileName, best.getType(), best.getScore(), second.getType(), second.getScore());
            }

            logger.info("Classified file {} as {} with score {}", fileName, best.getType(), best.getScore());
            out.put(fileName, new Classification(fileName, best.getType(), best.getScore(), best.getDocumentNumber(), ambiguous));
        }

        logger.info("Classification complete. Processed {} documents", out.size());
        return out;
    }

    public Score scorePan(String content) {
        String pan = keywordMatcher.validatePanCardInContent(content);
        if (pan == null) return new Score("PAN", 0, null);
        if ("PAN_KEYWORD_FOUND".equals(pan)) return new Score("PAN", 60, null);
        return new Score("PAN", 90, pan);
    }

    public Score scoreAadhaar(String content) {
        String a = keywordMatcher.validateAadharCardInContent(content);
        if (a == null) return new Score("AADHAAR", 0, null);
        if ("AADHAAR_KEYWORD_FOUND".equals(a)) return new Score("AADHAAR", 60, null);
        return new Score("AADHAAR", 90, a);
    }

    public Score scoreGst(String content) {
        String gst = keywordMatcher.validateGSTInContent(content);
        if (gst == null) return new Score("GST", 0, null);
        if ("GST_KEYWORD_FOUND".equals(gst)) return new Score("GST", 60, null);
        return new Score("GST", 90, gst);
    }

    public Score scoreIncomeTax(String content) {
        String it = keywordMatcher.validateIncomeTaxInContent(content);
        if (it == null) return new Score("INCOME_TAX", 0, null);

        String lower = content.toLowerCase();
        int score = 60;
        if (lower.contains("assessment year") || lower.contains("return of income") || lower.contains("itr")) {
            score = 80;
        }
        return new Score("INCOME_TAX", score, null);
    }

    public Score scoreExperience(String content) {
        String exp = keywordMatcher.validateExperienceCertificateInContent(content);
        if (exp == null) return new Score("EXPERIENCE", 0, null);
        return new Score("EXPERIENCE", 70, null);
    }

    public Score scoreCompanyReg(String content) {
        String cr = keywordMatcher.validateCompanyRegistrationInContent(content);
        if (cr == null) return new Score("COMPANY_REG", 0, null);
        if ("COMPANY_REG_FOUND".equals(cr)) return new Score("COMPANY_REG", 70, null);

        return new Score("COMPANY_REG", 90, cr);
    }

    public Score scoreInsurance(String content) {
        String ins = keywordMatcher.validateInsuranceInContent(content);
        if (ins == null) return new Score("INSURANCE", 0, null);
        return new Score("INSURANCE", 70, null);
    }

    public Map<String, List<Classification>> getClassificationsByType(Map<String, Classification> fileClassification) {
        Map<String, List<Classification>> byType = new HashMap<>();
        for (Classification c : fileClassification.values()) {
            byType.computeIfAbsent(c.getType(), k -> new ArrayList<>()).add(c);
        }
        for (List<Classification> list : byType.values()) {
            list.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        }
        return byType;
    }
}

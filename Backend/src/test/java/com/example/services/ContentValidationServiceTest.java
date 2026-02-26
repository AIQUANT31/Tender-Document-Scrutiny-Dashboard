package com.example.services;

import com.example.dto.ValidationResult;
import com.example.services.document.OcrService;
import com.example.services.document.TextExtractor;
import com.example.services.document.DocumentFinder;
import com.example.services.document.DocumentClassifier;
import com.example.services.document.DuplicateDetector;
import com.example.services.validation.ContentValidationService;
import com.example.services.validation.ContentValidationModels.Classification;
import com.example.services.validation.KeywordMatcher;
import com.example.services.validation.ValidationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentValidationServiceTest {

    @Mock
    private OcrService ocrService;

    @Spy
    private KeywordMatcher keywordMatcher = new KeywordMatcher();

    @Mock
    private DuplicateDetector duplicateDetector;

    @Mock
    private TextExtractor textExtractor;

    @Mock
    private DocumentFinder documentFinder;

    @Mock
    private DocumentClassifier documentClassifier;

    @Mock
    private ValidationHelper validationHelper;

    @InjectMocks
    private ContentValidationService contentValidationService;

    @BeforeEach
    void setup() {
        lenient().when(duplicateDetector.getDuplicateFileNames(any())).thenReturn(new ArrayList<>());
        
        lenient().when(textExtractor.extractTextFromFiles(any())).thenAnswer(invocation -> {
            MultipartFile[] files = invocation.getArgument(0);
            Map<String, String> result = new HashMap<>();
            if (files != null) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        String content = ocrService.extractText(file);
                        result.put(file.getOriginalFilename(), content);
                    }
                }
            }
            return result;
        });
    }

    @Test
    void validateWithRules_panKeywordShouldValidate() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("document.pdf");

        when(ocrService.extractText(any())).thenReturn("INCOME TAX DEPARTMENT - PAN CARD");

        List<String> required = Arrays.asList("PAN");
        ValidationResult result = contentValidationService.validateWithRules(required, new MultipartFile[]{file});

        assertTrue(result.isValid(), "PAN should validate when PAN keywords exist");
        assertTrue(result.getMissingDocuments().isEmpty());
        assertFalse(result.getMatchedDocuments().isEmpty());
    }

    @Test
    void validateWithRules_companyRegDocShouldNotValidatePanWithoutPanContext() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("company_reg.pdf");

        when(ocrService.extractText(any())).thenReturn(
                "CERTIFICATE OF INCORPORATION\n" +
                "Ministry of Corporate Affairs\n" +
                "CIN: U12345DL2010PLC123456\n" +
                "Director ID: ABCDE1234F\n"
        );

        ValidationResult result = contentValidationService.validateWithRules(Arrays.asList("PAN"), new MultipartFile[]{file});

        assertFalse(result.isValid(), "PAN should NOT validate from company registration doc without PAN context");
        assertEquals(Arrays.asList("PAN"), result.getMissingDocuments());
    }

    @Test
    void validateWithRules_gstDocShouldNotValidatePan() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("gst.pdf");

        when(ocrService.extractText(any())).thenReturn(
                "GST REGISTRATION CERTIFICATE\n" +
                "GSTIN: 27ABCDE1234F1Z5\n" +
                "Goods and Services Tax\n"
        );

        ValidationResult result = contentValidationService.validateWithRules(Arrays.asList("PAN", "GST Registration"), new MultipartFile[]{file});

        
        assertFalse(result.isValid());
        assertTrue(result.getMatchedDocuments().stream().anyMatch(s -> s.toLowerCase().contains("gst registration")));
        assertTrue(result.getMissingDocuments().stream().anyMatch(s -> s.toLowerCase().contains("pan")));
    }

    @Test
    void validateWithRules_whenNoMatchShouldBeMissing() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("random.pdf");

        when(ocrService.extractText(any())).thenReturn("This document is a bank statement.");

        List<String> required = Arrays.asList("PAN");
        ValidationResult result = contentValidationService.validateWithRules(required, new MultipartFile[]{file});

        assertFalse(result.isValid());
        assertEquals(Arrays.asList("PAN"), result.getMissingDocuments());
    }

    @Test
    void validateWithRules_aadhaarDocShouldValidateOnlyAadhaar() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("aadhaar.pdf");

        when(ocrService.extractText(any())).thenReturn(
                "Government of India\n" +
                "UIDAI\n" +
                "Aadhaar Number: 1234 5678 9012\n" +
                "Name: TEST USER\n" +
                "DOB: 01/01/1990\n"
        );

        ValidationResult result = contentValidationService.validateWithRules(
                Arrays.asList("Aadhaar Card", "PAN Card"),
                new MultipartFile[]{file}
        );

        
        assertFalse(result.isValid());
        assertTrue(result.getMatchedDocuments().stream().anyMatch(s -> s.toLowerCase().contains("aadhaar card")));
        assertTrue(result.getMissingDocuments().stream().anyMatch(s -> s.toLowerCase().contains("pan")));
    }

    @Test
    void validateWithRules_incomeTaxClearanceShouldValidateOnlyIncomeTax() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("income_tax.pdf");

        when(ocrService.extractText(any())).thenReturn(
                "INCOME TAX CLEARANCE CERTIFICATE\n" +
                "Return of Income\n" +
                "Assessment Year: 2024-25\n" +
                "This is to certify tax clearance.\n"
        );

        ValidationResult result = contentValidationService.validateWithRules(
                Arrays.asList("Income Tax Clearance", "GST Registration"),
                new MultipartFile[]{file}
        );

        
        assertFalse(result.isValid());
        assertTrue(result.getMatchedDocuments().stream().anyMatch(s -> s.toLowerCase().contains("income tax clearance")));
        assertTrue(result.getMissingDocuments().stream().anyMatch(s -> s.toLowerCase().contains("gst")));
    }

    @Test
    void validateWithRules_experienceCertificateShouldValidateOnlyExperience() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("experience.pdf");

        when(ocrService.extractText(any())).thenReturn(
                "EXPERIENCE CERTIFICATE\n" +
                "This is to certify that Mr. Test User worked as Software Engineer\n" +
                "From 01/01/2020 to 01/01/2024.\n" +
                "Designation: Software Engineer\n"
        );

        ValidationResult result = contentValidationService.validateWithRules(
                Arrays.asList("Experience Certificates", "Company Registration"),
                new MultipartFile[]{file}
        );

        assertFalse(result.isValid());
        assertTrue(result.getMatchedDocuments().stream().anyMatch(s -> s.toLowerCase().contains("experience certificates")));
        assertTrue(result.getMissingDocuments().stream().anyMatch(s -> s.toLowerCase().contains("company")));
    }

    @Test
    void validateWithRules_experienceCertificateShouldValidateEvenWithoutExactPhrase() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("scan_001.pdf");

        
        when(ocrService.extractText(any())).thenReturn(
                "To Whom It May Concern\n" +
                "This is to certify that Mr. Test User has worked as Site Engineer\n" +
                "from 01/01/2020 to 01/01/2024.\n" +
                "For ABC Infrastructure Pvt Ltd\n"
        );

        ValidationResult result = contentValidationService.validateWithRules(
                Arrays.asList("Experience Certificate"),
                new MultipartFile[]{file}
        );

        assertTrue(result.isValid(), "Experience should validate from strong employment phrasing + dates");
        assertTrue(result.getMissingDocuments().isEmpty());
        assertTrue(result.getMatchedDocuments().stream().anyMatch(s -> s.toLowerCase().contains("experience certificate")));
    }

    @Test
    void validateWithRules_insuranceCertificateShouldValidateOnlyInsurance() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("insurance.pdf");

        when(ocrService.extractText(any())).thenReturn(
                "INSURANCE CERTIFICATE\n" +
                "Policy Number: POL123456\n" +
                "Insurance Company: ABC Insurance Co\n" +
                "Valid From: 01/01/2025 Valid Until: 01/01/2026\n"
        );

        ValidationResult result = contentValidationService.validateWithRules(
                Arrays.asList("Insurance Certificate", "PAN Card"),
                new MultipartFile[]{file}
        );

        assertFalse(result.isValid());
        assertTrue(result.getMatchedDocuments().stream().anyMatch(s -> s.toLowerCase().contains("insurance certificate")));
        assertTrue(result.getMissingDocuments().stream().anyMatch(s -> s.toLowerCase().contains("pan")));
    }

    @Test
    void validateWithRules_companyRegShouldValidateOnlyCompanyRegEvenIfContainsPanLikeToken() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("company_reg.pdf");

        when(ocrService.extractText(any())).thenReturn(
                "CERTIFICATE OF INCORPORATION\n" +
                "Ministry of Corporate Affairs\n" +
                "CIN: U12345DL2010PLC123456\n" +
                "Director ID: ABCDE1234F\n"
        );

        ValidationResult result = contentValidationService.validateWithRules(
                Arrays.asList("Company Registration", "PAN Card"),
                new MultipartFile[]{file}
        );

        assertFalse(result.isValid());
        assertTrue(result.getMatchedDocuments().stream().anyMatch(s -> s.toLowerCase().contains("company registration")));
        assertTrue(result.getMissingDocuments().stream().anyMatch(s -> s.toLowerCase().contains("pan")));
    }
}

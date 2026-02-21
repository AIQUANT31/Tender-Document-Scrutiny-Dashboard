package com.example.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import com.example.dto.ValidationResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentValidationServiceTest {

    @Mock
    private OcrService ocrService;

    @Mock
    private DuplicateDetector duplicateDetector;

    @Mock
    private KeywordMatcher keywordMatcher;

    @InjectMocks
    private DocumentValidationService documentValidationService;

    @Mock
    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        // Setup duplicate detector to return empty list (no duplicates)
        when(duplicateDetector.getDuplicateFileNames(any())).thenReturn(new ArrayList<>());
    }

    @Test
    void testValidateWithRules_NoRequiredDocuments() {
        // Given
        List<String> requiredDocs = Arrays.asList();
        MultipartFile[] files = new MultipartFile[]{mockFile};

        // When
        ValidationResult result = 
            documentValidationService.validateWithRules(requiredDocs, files);

        // Then
        assertTrue(result.isValid());
        assertEquals("No required documents specified.", result.getMessage());
    }

    @Test
    void testValidateWithRules_NoFiles() {
        // Given
        List<String> requiredDocs = Arrays.asList("PAN");
        MultipartFile[] files = new MultipartFile[0];

        // When
        ValidationResult result = 
            documentValidationService.validateWithRules(requiredDocs, files);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getMissingDocuments().contains("PAN"));
    }

    @Test
    void testValidateWithRules_KeywordFound() throws IOException {
        // Given
        List<String> requiredDocs = Arrays.asList("PAN");
        List<String> keywords = Arrays.asList("pan", "permanent account", "income tax", "pan card");
        
        when(mockFile.getOriginalFilename()).thenReturn("pan_card.pdf");
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(ocrService.extractText(any())).thenReturn("This is a PAN card document with permanent account number");
        when(keywordMatcher.getKeywordsForDocument(any())).thenReturn(keywords);
        when(keywordMatcher.validatePanCardInContent(any())).thenReturn("PAN_KEYWORD_FOUND");

        // When
        ValidationResult result = 
            documentValidationService.validateWithRules(requiredDocs, new MultipartFile[]{mockFile});

        // Then
        assertTrue(result.isValid(), "Should be valid when keyword found");
        assertTrue(result.getMatchedDocuments().size() > 0, "Should have matched documents");
        assertTrue(result.getMissingDocuments().isEmpty(), "Should have no missing documents");
    }

    @Test
    void testValidateWithRules_KeywordNotFound() throws IOException {
        // Given
        List<String> requiredDocs = Arrays.asList("PASSPORT");
        List<String> keywords = Arrays.asList("passport", "travel document");
        
        when(mockFile.getOriginalFilename()).thenReturn("bank_statement.pdf");
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(ocrService.extractText(any())).thenReturn("This is a bank statement for account 12345");
        when(keywordMatcher.getKeywordsForDocument(any())).thenReturn(keywords);
        when(keywordMatcher.validatePanCardInContent(any())).thenReturn(null);
        when(keywordMatcher.validateAadharCardInContent(any())).thenReturn(null);
        when(keywordMatcher.checkContentMatch(any(), any(), any(), any())).thenReturn(false);

        // When
        ValidationResult result = 
            documentValidationService.validateWithRules(requiredDocs, new MultipartFile[]{mockFile});

        // Then
        assertFalse(result.isValid(), "Should be invalid when keyword not found");
        assertTrue(result.getMissingDocuments().contains("PASSPORT"), "Should have PASSPORT in missing documents");
    }

    @Test
    void testValidateWithRules_ImageConversion() throws IOException {
        // Given
        List<String> requiredDocs = Arrays.asList("PAN");
        List<String> keywords = Arrays.asList("pan", "permanent account");
        
        when(mockFile.getOriginalFilename()).thenReturn("pan_card.pdf");
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(ocrService.extractText(any())).thenReturn("PAN card document");
        when(keywordMatcher.getKeywordsForDocument(any())).thenReturn(keywords);
        when(keywordMatcher.validatePanCardInContent(any())).thenReturn("PAN_KEYWORD_FOUND");

        // When
        ValidationResult result = 
            documentValidationService.validateWithRules(requiredDocs, new MultipartFile[]{mockFile});

        // Then
        assertTrue(result.isValid(), "Should be valid when keyword found");
    }

    @Test
    void testValidateWithRules_CaseInsensitive() throws IOException {
        // Given
        List<String> requiredDocs = Arrays.asList("Pan"); // Capital P
        List<String> keywords = Arrays.asList("pan", "permanent account");
        
        when(mockFile.getOriginalFilename()).thenReturn("pan_card.pdf");
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getContentType()).thenReturn("application/pdf");
        when(ocrService.extractText(any())).thenReturn("PAN CARD document"); // Uppercase in content
        when(keywordMatcher.getKeywordsForDocument(any())).thenReturn(keywords);
        when(keywordMatcher.validatePanCardInContent(any())).thenReturn("PAN_KEYWORD_FOUND");

        // When
        ValidationResult result = 
            documentValidationService.validateWithRules(requiredDocs, new MultipartFile[]{mockFile});

        // Then
        assertTrue(result.isValid(), "Should match case-insensitively");
    }
}

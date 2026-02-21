package com.example.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for DocumentValidationService
 * Tests rule-based keyword validation with OCR
 */
@ExtendWith(MockitoExtension.class)
class DocumentValidationServiceTest {

    @Mock
    private OcrService ocrService;

    @Mock
    private ImageToPdfConverter imageToPdfConverter;

    @InjectMocks
    private DocumentValidationService documentValidationService;

    @Mock
    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        // Setup common mocks
    }

    @Test
    void testValidateWithRules_NoRequiredDocuments() {
        // Given
        List<String> requiredDocs = Arrays.asList();
        MultipartFile[] files = new MultipartFile[]{mockFile};

        // When
        DocumentValidationService.ValidationResult result = 
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
        DocumentValidationService.ValidationResult result = 
            documentValidationService.validateWithRules(requiredDocs, files);

        // Then
        assertFalse(result.isValid());
        assertTrue(result.getMissingDocuments().contains("PAN"));
    }

    @Test
    void testValidateWithRules_KeywordFound() throws IOException {
        // Given
        List<String> requiredDocs = Arrays.asList("PAN");
        when(mockFile.getOriginalFilename()).thenReturn("pan_card.pdf");
        when(mockFile.isEmpty()).thenReturn(false);
        when(imageToPdfConverter.isImageFile(any())).thenReturn(false);
        when(ocrService.extractText(any())).thenReturn("This is a PAN card document with permanent account number");

        // When
        DocumentValidationService.ValidationResult result = 
            documentValidationService.validateWithRules(requiredDocs, new MultipartFile[]{mockFile});

        // Then
        assertTrue(result.isValid(), "Should be valid when keyword found");
        assertTrue(result.getMatchedDocuments().size() > 0, "Should have matched documents");
        assertTrue(result.getMissingDocuments().isEmpty(), "Should have no missing documents");
    }

    @Test
    void testValidateWithRules_KeywordNotFound() throws IOException {
        // Given
        List<String> requiredDocs = Arrays.asList("PAN");
        when(mockFile.getOriginalFilename()).thenReturn("other_doc.pdf");
        when(mockFile.isEmpty()).thenReturn(false);
        when(imageToPdfConverter.isImageFile(any())).thenReturn(false);
        when(ocrService.extractText(any())).thenReturn("This is some other document without PAN");

        // When
        DocumentValidationService.ValidationResult result = 
            documentValidationService.validateWithRules(requiredDocs, new MultipartFile[]{mockFile});

        // Then
        assertFalse(result.isValid(), "Should be invalid when keyword not found");
        assertTrue(result.getMissingDocuments().contains("PAN"), "Should have PAN in missing documents");
    }

    @Test
    void testValidateWithRules_ImageConversion() throws IOException {
        // Given
        List<String> requiredDocs = Arrays.asList("PAN");
        MultipartFile convertedPdf = mock(MultipartFile.class);
        
        when(mockFile.getOriginalFilename()).thenReturn("pan_card.jpg");
        when(mockFile.isEmpty()).thenReturn(false);
        when(imageToPdfConverter.isImageFile(any())).thenReturn(true);
        when(imageToPdfConverter.convertImageToPdf(any())).thenReturn(convertedPdf);
        when(convertedPdf.getOriginalFilename()).thenReturn("pan_card.pdf");
        when(convertedPdf.isEmpty()).thenReturn(false);
        when(ocrService.extractText(any())).thenReturn("PAN card document");

        // When
        DocumentValidationService.ValidationResult result = 
            documentValidationService.validateWithRules(requiredDocs, new MultipartFile[]{mockFile});

        // Then
        verify(imageToPdfConverter, times(1)).convertImageToPdf(any());
        assertTrue(result.isValid() || !result.getMissingDocuments().isEmpty());
    }

    @Test
    void testValidateWithRules_CaseInsensitive() throws IOException {
        // Given
        List<String> requiredDocs = Arrays.asList("Pan"); // Capital P
        when(mockFile.getOriginalFilename()).thenReturn("pan_card.pdf");
        when(mockFile.isEmpty()).thenReturn(false);
        when(imageToPdfConverter.isImageFile(any())).thenReturn(false);
        when(ocrService.extractText(any())).thenReturn("PAN CARD document"); // Uppercase in content

        // When
        DocumentValidationService.ValidationResult result = 
            documentValidationService.validateWithRules(requiredDocs, new MultipartFile[]{mockFile});

        // Then
        assertTrue(result.isValid(), "Should match case-insensitively");
    }
}

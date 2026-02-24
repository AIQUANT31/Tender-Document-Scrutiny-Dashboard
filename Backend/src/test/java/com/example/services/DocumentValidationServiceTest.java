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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentValidationServiceTest {

    @Mock
    private DuplicateDetector duplicateDetector;

    @Mock
    private ContentValidationService contentValidationService;

    @InjectMocks
    private DocumentValidationService documentValidationService;

    @Mock
    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        
        when(duplicateDetector.getDuplicateFileNames(any())).thenReturn(new ArrayList<>());
    }

    @Test
    void testValidateWithRules_DelegatesToContentValidationService() {
        List<String> requiredDocs = Arrays.asList("PAN");
        MultipartFile[] files = new MultipartFile[]{mockFile};

        ValidationResult expected = new ValidationResult();
        expected.setValid(true);
        expected.setMessage("ok");

        when(contentValidationService.validateWithRules(requiredDocs, files)).thenReturn(expected);

        ValidationResult actual = documentValidationService.validateWithRules(requiredDocs, files);

        assertSame(expected, actual);
        verify(contentValidationService, times(1)).validateWithRules(requiredDocs, files);
    }

    @Test
    void testValidateDocumentContent_DelegatesToContentValidationService() {
        List<String> requiredDocs = Arrays.asList("PAN");
        MultipartFile[] files = new MultipartFile[]{mockFile};

        ValidationResult expected = new ValidationResult();
        expected.setValid(false);
        expected.setMessage("missing");

        when(contentValidationService.validateDocumentContent(requiredDocs, files)).thenReturn(expected);

        ValidationResult actual = documentValidationService.validateDocumentContent(requiredDocs, files);

        assertSame(expected, actual);
        verify(contentValidationService, times(1)).validateDocumentContent(requiredDocs, files);
    }

    @Test
    void testDetectDuplicates_DelegatesToDuplicateDetector() {
        MultipartFile[] files = new MultipartFile[]{mockFile};
        Map<String, List<String>> expected = new HashMap<>();
        expected.put("dup", Arrays.asList("a.pdf", "b.pdf"));

        when(duplicateDetector.detectDuplicates(files)).thenReturn(expected);

        Map<String, List<String>> actual = documentValidationService.detectDuplicates(files);
        assertSame(expected, actual);
        verify(duplicateDetector, times(1)).detectDuplicates(files);
    }

    @Test
    void testGetFileNames_ReturnsOriginalFilenames() {
        MultipartFile f1 = mock(MultipartFile.class);
        MultipartFile f2 = mock(MultipartFile.class);

        when(f1.getOriginalFilename()).thenReturn("a.pdf");
        when(f2.getOriginalFilename()).thenReturn("b.pdf");

        List<String> names = documentValidationService.getFileNames(new MultipartFile[]{f1, f2});
        assertEquals(Arrays.asList("a.pdf", "b.pdf"), names);
    }
}

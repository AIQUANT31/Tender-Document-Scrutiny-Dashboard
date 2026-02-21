package com.example.services;

import com.example.dto.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Main document validation service that orchestrates validation using
 * ContentValidationService for content-based document validation.
 * 
 * This service acts as a facade/entry point that delegates to specialized
 * validation implementations.
 */
@Service
public class DocumentValidationService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentValidationService.class);

    @Autowired
    private DuplicateDetector duplicateDetector;

    @Autowired
    private ContentValidationService contentValidationService;

    //  Duplicate Detection Methods 

    
    public Map<String, List<String>> detectDuplicates(MultipartFile[] files) {
        return duplicateDetector.detectDuplicates(files);
    }

    
    public List<String> getDuplicateFileNames(MultipartFile[] files) {
        return duplicateDetector.getDuplicateFileNames(files);
    }

    // Content-Based Validation =

   
    public ValidationResult validateDocumentContent(List<String> requiredDocuments, MultipartFile[] files) {
        logger.info("Delegating to ContentValidationService - CONTENT-BASED validation");
        return contentValidationService.validateDocumentContent(requiredDocuments, files);
    }

   
    public ValidationResult validateWithRules(List<String> requiredDocuments, MultipartFile[] files) {
        logger.info("Delegating to ContentValidationService - RULE-BASED validation");
        return contentValidationService.validateWithRules(requiredDocuments, files);
    }

    // ===Utility Methods ===

  
    public List<String> getFileNames(MultipartFile[] files) {
        List<String> fileNames = new ArrayList<>();
        
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && file.getOriginalFilename() != null) {
                    fileNames.add(file.getOriginalFilename());
                }
            }
        }
        
        return fileNames;
    }
}

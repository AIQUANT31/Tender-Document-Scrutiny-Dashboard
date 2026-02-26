package com.example.services.document;

import com.example.dto.ValidationResult;
import com.example.services.validation.ContentValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;


@Service
public class DocumentValidationService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentValidationService.class);

    @Autowired
    private DuplicateDetector duplicateDetector;

    @Autowired
    private ContentValidationService contentValidationService;

    

    

    public Map<String, List<String>> detectDuplicates(MultipartFile[] files) {
        return duplicateDetector.detectDuplicates(files);
    }

    
    public List<String> getDuplicateFileNames(MultipartFile[] files) {
        return duplicateDetector.getDuplicateFileNames(files);
    }

    

   
    public ValidationResult validateDocumentContent(List<String> requiredDocuments, MultipartFile[] files) {
        logger.info("Delegating to ContentValidationService - CONTENT-BASED validation");
        return contentValidationService.validateDocumentContent(requiredDocuments, files);
    }

   
    public ValidationResult validateWithRules(List<String> requiredDocuments, MultipartFile[] files) {
        logger.info("Delegating to ContentValidationService - RULE-BASED validation");
        return contentValidationService.validateWithRules(requiredDocuments, files);
    }

  

  
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

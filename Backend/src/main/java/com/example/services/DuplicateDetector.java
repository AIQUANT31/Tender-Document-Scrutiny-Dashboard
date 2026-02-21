package com.example.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;
import java.util.*;

//  Service for detecting duplicate documents based on file content hashing
@Service
public class DuplicateDetector {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateDetector.class);

   
    public Map<String, List<String>> detectDuplicates(MultipartFile[] files) {
        Map<String, List<String>> hashToFiles = new HashMap<>();
        
        if (files == null || files.length == 0) {
            return hashToFiles;
        }
        
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                try {
                    String hash = calculateFileHash(file);
                    String fileName = file.getOriginalFilename();
                    
                    hashToFiles.computeIfAbsent(hash, k -> new ArrayList<>()).add(fileName);
                    logger.info("File '{}' hash: {}", fileName, hash);
                } catch (Exception e) {
                    logger.warn("Could not calculate hash for file: {}", file.getOriginalFilename());
                }
            }
        }
        
        return hashToFiles;
    }

 
    private String calculateFileHash(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = file.getBytes();
            byte[] hashBytes = md.digest(fileBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.warn("Could not calculate hash: {}", e.getMessage());
            return "hash_error_" + System.currentTimeMillis();
        }
    }
  
    public List<String> getDuplicateFileNames(MultipartFile[] files) {
        List<String> duplicates = new ArrayList<>();
        Map<String, List<String>> hashToFiles = detectDuplicates(files);
        
        for (Map.Entry<String, List<String>> entry : hashToFiles.entrySet()) {
            if (entry.getValue().size() > 1) {
                // Keep the first file, mark rest as duplicates
                for (int i = 1; i < entry.getValue().size(); i++) {
                    duplicates.add(entry.getValue().get(i) + " (duplicate of " + entry.getValue().get(0) + ")");
                }
            }
        }
        
        return duplicates;
    }
}

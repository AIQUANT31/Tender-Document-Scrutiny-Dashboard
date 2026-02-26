package com.example.services.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Service
public class ZipFileService {

    private static final Logger logger = LoggerFactory.getLogger(ZipFileService.class);
    private static final String UPLOAD_DIR = "./bid-documents/";
    private static final long MAX_TOTAL_SIZE = 200 * 1024 * 1024; // 200MB max for ZIP
    private static final int MAX_FILES = 50;

    public java.util.Map<String, Object> createZipFromFiles(MultipartFile[] files, String prefix) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        // Validate input
        if (files == null || files.length == 0) {
            response.put("success", false);
            response.put("message", "No files provided for ZIP creation");
            return response;
        }

        if (files.length > MAX_FILES) {
            response.put("success", false);
            response.put("message", "Maximum " + MAX_FILES + " files allowed per ZIP");
            return response;
        }

        // Calculate total size
        long totalSize = 0;
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                totalSize += file.getSize();
            }
        }

        if (totalSize > MAX_TOTAL_SIZE) {
            response.put("success", false);
            response.put("message", "Total file size exceeds " + (MAX_TOTAL_SIZE / (1024 * 1024)) + "MB limit");
            return response;
        }

        if (totalSize == 0) {
            response.put("success", false);
            response.put("message", "All files are empty");
            return response;
        }

        try {
            // Create upload directory if not exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique ZIP filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
            String zipFileName = prefix + "_" + timestamp + "_" + randomSuffix + ".zip";
            Path zipPath = uploadPath.resolve(zipFileName);

            // Create ZIP file
            createZipFile(files, zipPath);

            // Verify ZIP was created
            if (Files.exists(zipPath) && Files.size(zipPath) > 0) {
                logger.info("ZIP file created successfully: {}", zipFileName);
                
                response.put("success", true);
                response.put("message", "ZIP file created successfully with " + files.length + " file(s)");
                response.put("zipFileName", zipFileName);
                response.put("zipFilePath", "/bid-documents/" + zipFileName);
                response.put("originalFileCount", files.length);
                response.put("totalSize", totalSize);
                response.put("zipFileSize", Files.size(zipPath));
            } else {
                throw new IOException("ZIP file was not created properly");
            }

        } catch (Exception e) {
            logger.error("Error creating ZIP file: ", e);
            response.put("success", false);
            response.put("message", "Error creating ZIP file: " + e.getMessage());
        }

        return response;
    }

   
    private void createZipFile(MultipartFile[] files, Path zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipPath)))) {
            byte[] buffer = new byte[8192]; // 8KB buffer
            
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                String originalFileName = sanitizeFileName(file.getOriginalFilename());
                
                // Add entry to ZIP
                ZipEntry zipEntry = new ZipEntry(originalFileName);
                zos.putNextEntry(zipEntry);

                // Write file content to ZIP
                InputStream is = new BufferedInputStream(file.getInputStream());
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    zos.write(buffer, 0, bytesRead);
                }
                
                is.close();
                zos.closeEntry();
                
                logger.debug("Added file to ZIP: {}", originalFileName);
            }
        }
    }


    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unnamed_file";
        }
        
        // Get only the filename without path
        String sanitized = fileName;
        int lastSlash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            sanitized = fileName.substring(lastSlash + 1);
        }
        
        // Remove any path traversal attempts
        sanitized = sanitized.replaceAll("\\.\\.", "");
        
        // Replace invalid characters with underscore
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        return sanitized;
    }

    
    public java.util.Map<String, Object> createZipFromExistingFiles(
            java.util.List<String> filePaths, String prefix) {
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        if (filePaths == null || filePaths.isEmpty()) {
            response.put("success", false);
            response.put("message", "No file paths provided");
            return response;
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String randomSuffix = String.format("%04d", (int) (Math.random() * 10000));
            String zipFileName = prefix + "_" + timestamp + "_" + randomSuffix + ".zip";
            Path zipPath = uploadPath.resolve(zipFileName);

            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipPath)))) {
                byte[] buffer = new byte[8192];
                
                for (String filePath : filePaths) {
                    Path sourcePath = Paths.get(filePath);
                    
                    if (Files.exists(sourcePath) && Files.isReadable(sourcePath)) {
                        String fileName = sourcePath.getFileName().toString();
                        ZipEntry zipEntry = new ZipEntry(fileName);
                        zos.putNextEntry(zipEntry);
                        
                        // Use buffer for consistent approach with createZipFile method
                        try (InputStream is = new BufferedInputStream(Files.newInputStream(sourcePath))) {
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                zos.write(buffer, 0, bytesRead);
                            }
                        }
                        zos.closeEntry();
                        
                        logger.debug("Added to ZIP: {}", fileName);
                    } else {
                        logger.warn("File not found or not readable: {}", filePath);
                    }
                }
            }

            response.put("success", true);
            response.put("message", "ZIP created successfully");
            response.put("zipFileName", zipFileName);
            response.put("zipFilePath", "/bid-documents/" + zipFileName);

        } catch (Exception e) {
            logger.error("Error creating ZIP from existing files: ", e);
            response.put("success", false);
            response.put("message", "Error creating ZIP: " + e.getMessage());
        }

        return response;
    }

    
    public long estimateZipSize(MultipartFile[] files) {
        if (files == null) return 0;
        
        long totalSize = 0;
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                totalSize += file.getSize();
            }
        }
        
        // Add overhead for ZIP structure (approximately 1-2% more)
        return (long) (totalSize * 1.02);
    }
}

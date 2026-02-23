package com.example.controller;

import com.example.entity.Bid;
import com.example.services.BidService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bids")
@CrossOrigin(origins = "*")
public class BidDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(BidDocumentController.class);
    private static final String UPLOAD_DIR = "./bid-documents/";

    @Autowired
    private BidService bidService;

    /**
     * Create bid with document
     */
    @PostMapping(value = "/create-with-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createBidWithDocument(
            @RequestParam("tenderId") Long tenderId,
            @RequestParam("bidderId") Long bidderId,
            @RequestParam("bidAmount") java.math.BigDecimal bidAmount,
            @RequestParam(value = "proposalText", required = false) String proposalText,
                        @RequestParam(value = "contactNumber", required = false) String contactNumber,
            @RequestParam(value = "status", defaultValue = "PENDING") String status,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create bid request
            com.example.dto.BidRequest request = new com.example.dto.BidRequest();
            request.setTenderId(tenderId);
            request.setBidderId(bidderId);
            request.setBidAmount(bidAmount);
            request.setProposalText(proposalText);
            request.setContactNumber(contactNumber);
            request.setStatus(status);
            
            // Create bid first
            Map<String, Object> bidResponse = bidService.createBid(request);
            
            if (!(Boolean) bidResponse.get("success")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(bidResponse);
            }
            
            Bid savedBid = (Bid) bidResponse.get("bid");
            
            // If file is provided, save it
            if (file != null && !file.isEmpty()) {
                // Validate and save file
                String docPath = validateAndSaveFile(file, savedBid.getId());
                if (docPath == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Invalid file type or size"));
                }
                
                // Update bid with document path
                savedBid.setDocumentPath(docPath);
                                bidService.saveBid(savedBid);
                
                bidResponse.put("documentPath", docPath);
            }
            
            return ResponseEntity.ok(bidResponse);
            
        } catch (Exception e) {
            logger.error("Error creating bid with document: ", e);
            response.put("success", false);
            response.put("message", "Error creating bid: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Add documents to existing bid
     */
    @PostMapping(value = "/add-documents/{bidId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> addDocumentsToBid(
            @PathVariable Long bidId,
            @RequestParam("files") MultipartFile[] files) {
        
        Map<String, Object> response = new HashMap<>();
        List<String> savedFileNames = new ArrayList<>();
        
        try {
            Bid bid = bidService.getBidById(bidId);
            if (bid == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Bid not found"));
            }
            
            // Get existing paths
            List<String> allPaths = parseDocumentPaths(bid.getDocumentPaths());

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                
                // Validate file
                if (!isValidPdfFile(file, 50 * 1024 * 1024)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Only PDF files under 50MB are allowed"));
                }

                // Save file
                String docPath = saveFile(file, "bid_" + bidId);
                if (docPath != null) {
                    savedFileNames.add(docPath);
                    allPaths.add(docPath);
                }
            }
            
            // Save as proper JSON array
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonPaths = mapper.writeValueAsString(allPaths);
            bid.setDocumentPaths(jsonPaths);
            bidService.saveBid(bid);
            
            response.put("success", true);
            response.put("message", "Documents uploaded successfully");
            response.put("fileNames", savedFileNames);
            response.put("documentPaths", jsonPaths);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error uploading documents: ", e);
            response.put("success", false);
            response.put("message", "Error uploading documents: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

 
    @PostMapping(value = "/upload-documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadDocuments(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "bidId", required = false) Long bidId) {
        
        Map<String, Object> response = new HashMap<>();
        List<String> savedFileNames = new ArrayList<>();
        
        logger.info("Uploading {} files", files != null ? files.length : 0);
        
        if (files == null || files.length == 0) {
            response.put("success", false);
            response.put("message", "No files provided");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                
                // Validate file
                if (!isValidPdfFile(file, 50 * 1024 * 1024)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Only PDF files under 50MB are allowed"));
                }

                // Save file to disk
                String docPath = saveFile(file, "bid_" + (bidId != null ? bidId : "temp"));
                if (docPath != null) {
                    savedFileNames.add(docPath);
                    logger.info("File saved: {}", file.getOriginalFilename());
                }
            }

            // If bidId provided, update bid with document paths
            if (bidId != null) {
                Bid bid = bidService.getBidById(bidId);
                if (bid != null) {
                    List<String> allPaths = parseDocumentPaths(bid.getDocumentPaths());
                    allPaths.addAll(savedFileNames);
                    
                    // Save as proper JSON array
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String jsonPaths = mapper.writeValueAsString(allPaths);
                    bid.setDocumentPaths(jsonPaths);
                    bidService.saveBid(bid);
                }
            }
            
            response.put("success", true);
            response.put("message", "Documents uploaded successfully");
            response.put("fileNames", savedFileNames);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error uploading documents: ", e);
            response.put("success", false);
            response.put("message", "Error uploading documents: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

   //upload single document to a bid    
    @PostMapping("/upload-document")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bidId") Long bidId) {
        
        try {
            // Validate file
            if (!isValidPdfFile(file, 10 * 1024 * 1024)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "Only PDF files under 10MB are allowed"));
            }

            // Save file
            String docPath = saveFile(file, "bid_" + bidId);
            if (docPath == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error saving file"));
            }

            // Update bid with document path
            Bid bid = bidService.getBidById(bidId);
            if (bid == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Bid not found"));
            }

            bid.setDocumentPath(docPath);
            bidService.saveBid(bid);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Document uploaded successfully",
                "documentPath", docPath
            ));

        } catch (IOException e) {
            logger.error("Error uploading document: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error uploading document"));
        }
    }

// Download uploaded Document  
    @GetMapping("/download-document/{bidId}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long bidId, 
            @RequestParam(value = "fileName", required = false) String fileName) {
        try {
            Bid bid = bidService.getBidById(bidId);
            if (bid == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath;
            String downloadFileName;

            // If specific filename provided, try to find it in documentPaths
            if (fileName != null && !fileName.isEmpty()) {
                String docPaths = bid.getDocumentPaths();
                if (docPaths != null && docPaths.contains(fileName)) {
                    filePath = Paths.get("./bid-documents/" + fileName);
                    downloadFileName = fileName;
                } else if (bid.getDocumentPath() != null && bid.getDocumentPath().contains(fileName)) {
                    filePath = Paths.get("." + bid.getDocumentPath());
                    downloadFileName = bid.getDocumentPath().substring(bid.getDocumentPath().lastIndexOf("/") + 1);
                } else {
                    return ResponseEntity.notFound().build();
                }
            } else {
                // Default: use documentPath
                if (bid.getDocumentPath() == null) {
                    return ResponseEntity.notFound().build();
                }
                filePath = Paths.get("." + bid.getDocumentPath());
                downloadFileName = bid.getDocumentPath().substring(bid.getDocumentPath().lastIndexOf("/") + 1);
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + downloadFileName + "\"")
                    .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== Helper Methods ====================

    private String validateAndSaveFile(MultipartFile file, Long bidId) throws IOException {
        // Validate file type
        if (!file.getContentType().equals("application/pdf")) {
            return null;
        }

        // Validate file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return null;
        }

        return saveFile(file, "bid_" + bidId);
    }

    private boolean isValidPdfFile(MultipartFile file, long maxSize) {
        return file.getContentType().equals("application/pdf") && file.getSize() <= maxSize;
    }

    private String saveFile(MultipartFile file, String prefix) throws IOException {
        // Create upload directory if not exists
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename with timestamp and random number
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = prefix + "_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000) + fileExtension;

        // Save file
        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/bid-documents/" + newFilename;
    }

    private List<String> parseDocumentPaths(String existingPaths) {
        List<String> allPaths = new ArrayList<>();
        if (existingPaths != null && !existingPaths.isEmpty()) {
            try {
                // Try to parse as JSON
                List<?> parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue(existingPaths, List.class);
                for (Object p : parsed) {
                    allPaths.add(p.toString());
                }
            } catch (Exception e) {
                // If not JSON, try comma-separated
                String clean = existingPaths.replace("[", "").replace("\"", "").replace("]", "");
                for (String p : clean.split(",")) {
                    if (!p.trim().isEmpty()) allPaths.add(p.trim());
                }
            }
        }
        return allPaths;
    }
}

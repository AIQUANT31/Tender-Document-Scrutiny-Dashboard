package com.example.controller;

import com.example.dto.BidRequest;
import com.example.dto.BidWithBidderResponse;
import com.example.dto.DocumentValidationResponse;
import com.example.dto.ValidationResult;
import com.example.entity.Bid;
import com.example.services.BidService;
import com.example.services.DocumentValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bids")
@CrossOrigin(origins = "*")
public class BidController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BidController.class);

    @Autowired
    private BidService bidService;

    @Autowired
    private DocumentValidationService documentValidationService;

    

    /**
     * Validate document CONTENT using OCR - This checks actual PDF content!
     * Uses rule-based keyword matching on extracted text
     */
    @PostMapping("/validate-content")
    public ResponseEntity<DocumentValidationResponse> validateDocumentContent(
            @RequestParam("requiredDocuments") String requiredDocs,
            @RequestParam("files") MultipartFile[] files) {
        
        try {
            List<String> requiredDocuments = new ArrayList<>();
            if (requiredDocs != null && !requiredDocs.isEmpty()) {
                for (String doc : requiredDocs.split(",")) {
                    requiredDocuments.add(doc.trim());
                }
            }
            
            logger.info("Content validation requested for: {} documents", requiredDocuments.size());
            
            if (files == null || files.length == 0) {
                DocumentValidationResponse response = new DocumentValidationResponse();
                response.setValid(false);
                response.setMessage("No files provided");
                return ResponseEntity.ok(response);
            }
            
            // Validate that all files are PDFs
            List<String> invalidFiles = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String contentType = file.getContentType();
                    String fileName = file.getOriginalFilename();
                    
                    // Check content type
                    if (contentType == null || !contentType.equals("application/pdf")) {
                        // Also check file extension as fallback
                        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                            invalidFiles.add(fileName != null ? fileName : "unknown");
                        }
                    }
                }
            }
            
            if (!invalidFiles.isEmpty()) {
                DocumentValidationResponse response = new DocumentValidationResponse();
                response.setValid(false);
                response.setMessage("Only PDF files are allowed. Invalid files: " + String.join(", ", invalidFiles));
                logger.warn("Rejected non-PDF files: {}", invalidFiles);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            ValidationResult result = 
                documentValidationService.validateDocumentContent(requiredDocuments, files);
            
            DocumentValidationResponse response = new DocumentValidationResponse();
            response.setValid(result.isValid());
            response.setMessage(result.getMessage());
            response.setMatchedDocuments(result.getMatchedDocuments());
            response.setMissingDocuments(result.getMissingDocuments());
            response.setWarnings(result.getWarnings());
            response.setDuplicateDocuments(result.getDuplicateDocuments());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error in content validation: ", e);
            DocumentValidationResponse response = new DocumentValidationResponse();
            response.setValid(false);
            response.setMessage("Validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Validate with rules - combines filename and content validation
     */
    @PostMapping("/validate-with-rules")
    public ResponseEntity<DocumentValidationResponse> validateWithRules(
            @RequestParam("requiredDocuments") String requiredDocs,
            @RequestParam("files") MultipartFile[] files) {
        
        try {
            logger.info("validate-with-rules called with files: {}", files != null ? files.length : 0);
            
            List<String> requiredDocuments = new ArrayList<>();
            if (requiredDocs != null && !requiredDocs.isEmpty()) {
                for (String doc : requiredDocs.split(",")) {
                    requiredDocuments.add(doc.trim());
                }
            }
            
            logger.info("Rule-based validation requested for: {}", requiredDocuments);
            
            if (files == null || files.length == 0) {
                DocumentValidationResponse response = new DocumentValidationResponse();
                response.setValid(false);
                response.setMessage("No files provided");
                return ResponseEntity.ok(response);
            }
            
            // Validate that all files are PDFs
            List<String> invalidFiles = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String contentType = file.getContentType();
                    String fileName = file.getOriginalFilename();
                    
                    // Check content type
                    if (contentType == null || !contentType.equals("application/pdf")) {
                        // Also check file extension as fallback
                        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                            invalidFiles.add(fileName != null ? fileName : "unknown");
                        }
                    }
                }
            }
            
            if (!invalidFiles.isEmpty()) {
                DocumentValidationResponse response = new DocumentValidationResponse();
                response.setValid(false);
                response.setMessage("Only PDF files are allowed. Invalid files: " + String.join(", ", invalidFiles));
                logger.warn("Rejected non-PDF files: {}", invalidFiles);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            ValidationResult result = 
                documentValidationService.validateWithRules(requiredDocuments, files);
            
            DocumentValidationResponse response = new DocumentValidationResponse();
            response.setValid(result.isValid());
            response.setMessage(result.getMessage());
            response.setMatchedDocuments(result.getMatchedDocuments());
            response.setMissingDocuments(result.getMissingDocuments());
            response.setWarnings(result.getWarnings());
            response.setDuplicateDocuments(result.getDuplicateDocuments());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error in rule-based validation: ", e);
            DocumentValidationResponse response = new DocumentValidationResponse();
            response.setValid(false);
            response.setMessage("Validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createBid(@RequestBody BidRequest request) {
        Map<String, Object> response = bidService.createBid(request);
        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Get all bids for a specific tender
    @GetMapping("/tender/{tenderId}")
    public ResponseEntity<List<Bid>> getBidsByTender(@PathVariable Long tenderId) {
        List<Bid> bids = bidService.getBidsByTender(tenderId);
        return ResponseEntity.ok(bids);
    }

    //  Get all bids from a specific bidder
    @GetMapping("/bidder/{bidderId}")
    public ResponseEntity<List<Bid>> getBidsByBidder(@PathVariable Long bidderId) {
        List<Bid> bids = bidService.getBidsByBidder(bidderId);
        return ResponseEntity.ok(bids);
    }

     //    Get bids with tender details
    @GetMapping("/bidder/{bidderId}/with-tenders")
    public ResponseEntity<List<com.example.dto.BidWithTenderResponse>> getBidsWithTenderDetails(
            @PathVariable Long bidderId) {
        List<com.example.dto.BidWithTenderResponse> bids = bidService.getBidsWithTenderDetails(bidderId);
        return ResponseEntity.ok(bids);
    }

//   Get a specific bid by ID
    @GetMapping("/{id}")
    public ResponseEntity<Bid> getBidById(@PathVariable Long id) {
        Bid bid = bidService.getBidById(id);
        if (bid != null) {
            return ResponseEntity.ok(bid);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Get bid statistics for a tender
    @GetMapping("/tender/{tenderId}/stats")
    public ResponseEntity<Map<String, Object>> getBidStats(@PathVariable Long tenderId) {
        Map<String, Object> stats = bidService.getBidStats(tenderId);
        return ResponseEntity.ok(stats);
    }

//  Get bids with bidder details for a tender
    @GetMapping("/tender/{tenderId}/with-bidders")
    public ResponseEntity<List<BidWithBidderResponse>> getBidsWithBidderDetails(@PathVariable Long tenderId) {
        List<BidWithBidderResponse> bids = bidService.getBidsWithBidderDetails(tenderId);
        return ResponseEntity.ok(bids);
    }

//    update bid status
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateBidStatus(
            @PathVariable Long id, 
            @RequestParam String status) {
        Map<String, Object> response = bidService.updateBidStatus(id, status);
        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

//    delete bid 
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBid(
            @PathVariable Long id, 
            @RequestParam Long userId) {
        Map<String, Object> response = bidService.deleteBid(id, userId);
        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}

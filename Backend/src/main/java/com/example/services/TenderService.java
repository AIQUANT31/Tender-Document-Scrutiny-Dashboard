package com.example.services;

import com.example.dto.TenderRequest;
import com.example.entity.Tender;
import com.example.repository.TenderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class TenderService {
    
    private static final Logger logger = LoggerFactory.getLogger(TenderService.class);

    @Autowired
    private TenderRepository tenderRepository;

    
    @CacheEvict(value = {"allTenders", "tendersByUser"}, allEntries = true)
    public Map<String, Object> createTender(TenderRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Creating tender with name: {}, description: {}, createdBy: {}", 
                request.getName(), request.getDescription(), request.getCreatedBy());

            // Validate required fields
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Tender name is required");
                return response;
            }
            
            if (request.getCreatedBy() == null) {
                response.put("success", false);
                response.put("message", "User ID is required");
                return response;
            }

            Tender tender = new Tender();
            tender.setName(request.getName());
            tender.setDescription(request.getDescription());
            tender.setCreatedBy(request.getCreatedBy());
            tender.setStatus("OPEN");
            tender.setBudget(request.getBudget());
            tender.setDeadline(request.getDeadline());
            tender.setRequiredDocuments(request.getRequiredDocuments());
            tender.setCategory(request.getCategory());
            tender.setLocation(request.getLocation());
            tender.setOpeningDate(request.getOpeningDate());
            tender.setContactNumber(request.getContactNumber());
            tender.setIsWhatsapp(request.getIsWhatsapp());
            tender.setComments(request.getComments());
            tender.setUserType(request.getUserType());

            Tender savedTender = tenderRepository.save(tender);
            logger.info("Tender created successfully with id: {}", savedTender.getId());

            response.put("success", true);
            response.put("message", "Tender created successfully!");
            
            Map<String, Object> tenderMap = new HashMap<>();
            tenderMap.put("id", savedTender.getId());
            tenderMap.put("name", savedTender.getName());
            tenderMap.put("description", savedTender.getDescription());
            tenderMap.put("createdBy", savedTender.getCreatedBy());
            tenderMap.put("createdAt", savedTender.getCreatedAt() != null ? savedTender.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            tenderMap.put("updatedAt", savedTender.getUpdatedAt() != null ? savedTender.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            tenderMap.put("status", savedTender.getStatus());
            tenderMap.put("budget", savedTender.getBudget() != null ? savedTender.getBudget().doubleValue() : 0.0);
            tenderMap.put("deadline", savedTender.getDeadline() != null ? savedTender.getDeadline().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            tenderMap.put("requiredDocuments", savedTender.getRequiredDocuments() != null ? savedTender.getRequiredDocuments() : "");
            tenderMap.put("category", savedTender.getCategory() != null ? savedTender.getCategory() : "");
            tenderMap.put("location", savedTender.getLocation() != null ? savedTender.getLocation() : "");
            tenderMap.put("openingDate", savedTender.getOpeningDate() != null ? savedTender.getOpeningDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
            tenderMap.put("contactNumber", savedTender.getContactNumber() != null ? savedTender.getContactNumber() : "");
            tenderMap.put("isWhatsapp", savedTender.getIsWhatsapp() != null ? savedTender.getIsWhatsapp() : false);
            tenderMap.put("comments", savedTender.getComments() != null ? savedTender.getComments() : "");
            tenderMap.put("userType", savedTender.getUserType() != null ? savedTender.getUserType() : "");
            
            response.put("tender", tenderMap);

            return response;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            logger.error("Data integrity violation creating tender: ", e);
            String errorMessage = e.getMostSpecificCause().getMessage();
            if (errorMessage != null && errorMessage.toLowerCase().contains("foreign key")) {
                response.put("success", false);
                response.put("message", "Invalid user. Please login again.");
            } else {
                response.put("success", false);
                response.put("message", "Data integrity error: " + errorMessage);
            }
            return response;
        } catch (Exception e) {
            logger.error("Error creating tender: ", e);
            response.put("success", false);
            response.put("message", "Error creating tender: " + e.getMessage());
            return response;
        }
    }

   
    @Cacheable(value = "allTenders", unless = "#result == null")
    public List<Tender> getAllTenders() {
        logger.info("Fetching all tenders (Cache Miss - loading from DB)");
        List<Tender> tenders = tenderRepository.findAll();
        logger.info("Found {} tenders", tenders.size());
        return tenders;
    }

    @Cacheable(value = "tendersByUser", key = "#userId",
              condition = "#userId != null",
              unless = "#result == null")
    public List<Tender> getTendersByUser(Long userId) {
        logger.debug("Fetching tenders for user: {} (Cache Miss - loading from DB)", userId);
        return tenderRepository.findByCreatedByOrderByCreatedAtDesc(userId);
    }

   
    @Cacheable(value = "tenderById", key = "#id",
              condition = "#id != null",
              unless = "#result == null")
    public Tender getTenderById(Long id) {
        logger.debug("Fetching tender by ID: {} (Cache Miss - loading from DB)", id);
        return tenderRepository.findById(id).orElse(null);
    }

    
    @CacheEvict(value = {"allTenders", "tenderById", "tendersByUser"}, allEntries = true)
    public Map<String, Object> deleteTender(Long id, Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        Tender tender = tenderRepository.findById(id).orElse(null);
        if (tender == null) {
            response.put("success", false);
            response.put("message", "Tender not found");
            return response;
        }
        
        if (!tender.getCreatedBy().equals(userId)) {
            response.put("success", false);
            response.put("message", "You can only delete your own tenders");
            return response;
        }
        
        tenderRepository.delete(tender);
        logger.info("Tender {} deleted by user {}", id, userId);
        
        response.put("success", true);
        response.put("message", "Deleted successfully");
        return response;
    }

  
    @CacheEvict(value = {"allTenders", "tenderById", "tendersByUser"}, allEntries = true)
    public Map<String, Object> updateComments(Long id, Long userId, String comments) {
        Map<String, Object> response = new HashMap<>();
        
        Tender tender = tenderRepository.findById(id).orElse(null);
        if (tender == null) {
            response.put("success", false);
            response.put("message", "Tender not found");
            return response;
        }
        
        if (!tender.getCreatedBy().equals(userId)) {
            response.put("success", false);
            response.put("message", "You can only update your own tenders");
            return response;
        }
        
        tender.setComments(comments);
        tenderRepository.save(tender);
        logger.info("Tender {} comments updated by user {}", id, userId);
        
        response.put("success", true);
        response.put("message", "Comments updated successfully");
        response.put("comments", comments);
        return response;
    }
}

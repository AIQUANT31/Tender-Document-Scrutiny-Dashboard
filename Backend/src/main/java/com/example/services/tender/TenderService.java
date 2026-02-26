package com.example.services.tender;

import com.example.dto.TenderRequest;
import com.example.entity.Tender;
import com.example.repository.TenderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class TenderService {
    
    private static final Logger logger = LoggerFactory.getLogger(TenderService.class);

    @Autowired
    private TenderRepository tenderRepository;

    
    @CacheEvict(value = {"allTenders", "tendersByUser", "dashboardData"}, allEntries = true)
    public Map<String, Object> createTender(TenderRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("Creating tender with name: {}, description: {}, createdBy: {}", 
                request.getName(), request.getDescription(), request.getCreatedBy());

            
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
            
            
            String initialStatus = determineStatusBasedOnDeadline(request.getDeadline());
            tender.setStatus(initialStatus);
            
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
        List<Tender> tenders = tenderRepository.findAllTendersSorted();
        
        
        updateTenderStatusesBasedOnDeadline(tenders);
        
        logger.info("Found {} tenders", tenders.size());
        return tenders;
    }

    @Cacheable(value = "tendersByUser", key = "#userId",
              condition = "#userId != null",
              unless = "#result == null")
    public List<Tender> getTendersByUser(Long userId) {
        logger.debug("Fetching tenders for user: {} (Cache Miss - loading from DB)", userId);
        List<Tender> tenders = tenderRepository.findByCreatedByOrderByCreatedAtDesc(userId);
        
        
        updateTenderStatusesBasedOnDeadline(tenders);
        
        return tenders;
    }

   
    @Cacheable(value = "tenderById", key = "#id",
              condition = "#id != null",
              unless = "#result == null")
    public Tender getTenderById(Long id) {
        logger.debug("Fetching tender by ID: {} (Cache Miss - loading from DB)", id);
        Tender tender = tenderRepository.findById(id).orElse(null);
        
        
        if (tender != null) {
            updateTenderStatusIfExpired(tender);
        }
        
        return tender;
    }

    
    @CacheEvict(value = {"allTenders", "tenderById", "tendersByUser", "dashboardData"}, allEntries = true)
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

  
    @CacheEvict(value = {"allTenders", "tenderById", "tendersByUser", "dashboardData"}, allEntries = true)
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
    
    @CacheEvict(value = {"allTenders", "tenderById", "tendersByUser", "dashboardData"}, allEntries = true)
    public Map<String, Object> updateTenderStatus(Long id, String status) {
        Map<String, Object> response = new HashMap<>();
        
        
        if (status == null || status.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Status is required");
            return response;
        }
        
        
        String upperStatus = status.toUpperCase();
        if (!"OPEN".equals(upperStatus) && !"CLOSED".equals(upperStatus)) {
            response.put("success", false);
            response.put("message", "Invalid status. Only OPEN or CLOSED allowed");
            return response;
        }
        
        Tender tender = tenderRepository.findById(id).orElse(null);
        if (tender == null) {
            response.put("success", false);
            response.put("message", "Tender not found");
            return response;
        }
        
        String oldStatus = tender.getStatus();
        tender.setStatus(upperStatus);
        tenderRepository.save(tender);
        logger.info("Tender {} status updated from {} to {}", id, oldStatus, upperStatus);
        
        response.put("success", true);
        response.put("message", "Status updated successfully");
        response.put("status", upperStatus);
        return response;
    }
    
    
    private String determineStatusBasedOnDeadline(LocalDateTime deadline) {
        if (deadline == null) {
            return "OPEN";
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        
        if (deadline.isBefore(now) || deadline.isEqual(now)) {
            return "CLOSED";
        }
        return "OPEN";
    }
    
    
    private void updateTenderStatusIfExpired(Tender tender) {
        if (tender == null || tender.getStatus() == null || !"OPEN".equals(tender.getStatus())) {
            return; 
        }
        
        if (tender.getDeadline() == null) {
            return; 
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        if (tender.getDeadline().isBefore(now) || tender.getDeadline().isEqual(now)) {
            String oldStatus = tender.getStatus();
            tender.setStatus("CLOSED");
            tenderRepository.save(tender);
            logger.info("Auto-updated tender {} status from {} to CLOSED (deadline: {})", 
                tender.getId(), oldStatus, tender.getDeadline());
        }
    }
    
    
    private void updateTenderStatusesBasedOnDeadline(List<Tender> tenders) {
        if (tenders == null || tenders.isEmpty()) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        int updatedCount = 0;
        
        for (Tender tender : tenders) {
            if (tender.getStatus() != null && "OPEN".equals(tender.getStatus()) && 
                tender.getDeadline() != null) {
                
                
                if (tender.getDeadline().isBefore(now) || tender.getDeadline().isEqual(now)) {
                    tender.setStatus("CLOSED");
                    tenderRepository.save(tender);
                    updatedCount++;
                }
            }
        }
        
        if (updatedCount > 0) {
            logger.info("Auto-updated {} tender(s) status to CLOSED based on deadline", updatedCount);
        }
    }
    
    
    
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 * * * *")
    @CacheEvict(value = {"allTenders", "tenderById", "tendersByUser", "dashboardData"}, allEntries = true)
    public void closeExpiredTenders() {
        logger.info("Running scheduled task to close expired tenders...");
        
        LocalDateTime now = LocalDateTime.now();
        
        
        List<Tender> expiredTenders = tenderRepository.findOpenTendersWithExpiredDeadline(now);
        
        if (!expiredTenders.isEmpty()) {
            logger.info("Found {} tenders with expired deadlines", expiredTenders.size());
            for (Tender tender : expiredTenders) {
                String oldStatus = tender.getStatus();
                tender.setStatus("CLOSED");
                tenderRepository.save(tender);
                logger.info("Automatically closed tender {} - deadline was {} (status changed from {} to CLOSED)", 
                    tender.getId(), tender.getDeadline(), oldStatus);
            }
        } else {
            logger.info("No expired tenders found");
        }
    }
}

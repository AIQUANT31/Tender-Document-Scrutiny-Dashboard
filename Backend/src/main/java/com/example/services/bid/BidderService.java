package com.example.services.bid;

import com.example.dto.BidderRequest;
import com.example.entity.Bidder;
import com.example.repository.BidderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class BidderService {

    private static final Logger logger = LoggerFactory.getLogger(BidderService.class);

    @Autowired
    private BidderRepository bidderRepository;

   
    @CacheEvict(value = {"allBidders", "biddersByUser", "bidderStats", "dashboardData"}, allEntries = true)
    public Map<String, Object> createBidder(BidderRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
           
            if (bidderRepository.existsByEmail(request.getEmail())) {
                response.put("success", false);
                response.put("message", "Email already exists");
                return response;
            }

            Bidder bidder = new Bidder();
            bidder.setCompanyName(request.getCompanyName());
            bidder.setEmail(request.getEmail());
            bidder.setPhone(request.getPhone());
            bidder.setType(request.getType());
            bidder.setCreatedBy(request.getCreatedBy());
            bidder.setAddress(request.getAddress());
            bidder.setContactPerson(request.getContactPerson());
            bidder.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
            bidder.setTotalBids(request.getTotalBids() != null ? request.getTotalBids() : 0);
            bidder.setWinningBids(request.getWinningBids() != null ? request.getWinningBids() : 0);

            Bidder savedBidder = bidderRepository.save(bidder);
            
            response.put("success", true);
            response.put("message", "Bidder created successfully");
            response.put("bidder", savedBidder);
            logger.info("Bidder created successfully with ID: {}", savedBidder.getId());
            
        } catch (Exception e) {
            logger.error("Error creating bidder: ", e);
            response.put("success", false);
            response.put("message", "Error creating bidder: " + e.getMessage());
        }
        
        return response;
    }

    
    @Cacheable(value = "allBidders", unless = "#result == null")
    public List<Bidder> getAllBidders() {
        logger.debug("Fetching all bidders (Cache Miss - loading from DB)");
        return bidderRepository.findAll();
    }

 
    @Cacheable(value = "biddersByUser", key = "#userId", 
              condition = "#userId != null", 
              unless = "#result == null")
    public List<Bidder> getBiddersByUser(Long userId) {
        logger.debug("Fetching bidders for user: {} (Cache Miss - loading from DB)", userId);
        return bidderRepository.findByCreatedBy(userId);
    }

    
    @Cacheable(value = "bidderById", key = "#id",
              condition = "#id != null",
              unless = "#result == null")
    public Bidder getBidderById(Long id) {
        logger.debug("Fetching bidder by ID: {} (Cache Miss - loading from DB)", id);
        return bidderRepository.findById(id).orElse(null);
    }

    
    @CacheEvict(value = {"allBidders", "bidderById", "biddersByUser", "bidderStats", "dashboardData"}, allEntries = true)
    public Map<String, Object> updateBidder(Long id, Long userId, BidderRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Bidder bidder = bidderRepository.findById(id).orElse(null);
            if (bidder == null) {
                response.put("success", false);
                response.put("message", "Bidder not found");
                return response;
            }

            
            if (userId == null || !bidder.getCreatedBy().equals(userId)) {
                response.put("success", false);
                response.put("message", "You are not authorized to edit this bidder");
                return response;
            }

            if (request.getEmail() != null && !request.getEmail().equals(bidder.getEmail())) {
                if (bidderRepository.existsByEmail(request.getEmail())) {
                    response.put("success", false);
                    response.put("message", "Email already exists");
                    return response;
                }
                bidder.setEmail(request.getEmail());
            }

            if (request.getCompanyName() != null) {
                bidder.setCompanyName(request.getCompanyName());
            }
            if (request.getPhone() != null) {
                bidder.setPhone(request.getPhone());
            }
            if (request.getType() != null) {
                bidder.setType(request.getType());
            }
            if (request.getAddress() != null) {
                bidder.setAddress(request.getAddress());
            }
            if (request.getContactPerson() != null) {
                bidder.setContactPerson(request.getContactPerson());
            }
            if (request.getStatus() != null) {
                bidder.setStatus(request.getStatus());
            }
            if (request.getTotalBids() != null) {
                bidder.setTotalBids(request.getTotalBids());
            }
            if (request.getWinningBids() != null) {
                bidder.setWinningBids(request.getWinningBids());
            }

            Bidder updatedBidder = bidderRepository.save(bidder);
            
            response.put("success", true);
            response.put("message", "Bidder updated successfully");
            response.put("bidder", updatedBidder);
            logger.info("Bidder updated successfully with ID: {}", id);
            
        } catch (Exception e) {
            logger.error("Error updating bidder: ", e);
            response.put("success", false);
            response.put("message", "Error updating bidder: " + e.getMessage());
        }
        
        return response;
    }

    @CacheEvict(value = {"allBidders", "bidderById", "biddersByUser", "bidderStats", "dashboardData"}, allEntries = true)
    public Map<String, Object> deleteBidder(Long id, Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Bidder bidder = bidderRepository.findById(id).orElse(null);
            if (bidder == null) {
                response.put("success", false);
                response.put("message", "Bidder not found");
                return response;
            }

            if (!bidder.getCreatedBy().equals(userId)) {
                response.put("success", false);
                response.put("message", "You are not authorized to delete this bidder");
                return response;
            }

            bidderRepository.delete(bidder);
            
            response.put("success", true);
            response.put("message", "Bidder deleted successfully");
            logger.info("Bidder deleted successfully with ID: {}", id);
            
        } catch (Exception e) {
            logger.error("Error deleting bidder: ", e);
            response.put("success", false);
            response.put("message", "Error deleting bidder: " + e.getMessage());
        }
        
        return response;
    }

   
    @Cacheable(value = "bidderStats", unless = "#result == null")
    public Map<String, Object> getBidderStats() {
        logger.debug("Fetching bidder stats (Cache Miss - loading from DB)");
        Map<String, Object> stats = new HashMap<>();
        
        try {
            List<Bidder> allBidders = bidderRepository.findAll();
            
            int totalBidders = allBidders.size();
            long activeBidders = allBidders.stream()
                .filter(b -> "ACTIVE".equals(b.getStatus()))
                .count();
            int totalWinningBids = allBidders.stream()
                .mapToInt(b -> b.getWinningBids() != null ? b.getWinningBids() : 0)
                .sum();
            double activeRate = totalBidders > 0 ? (double) activeBidders / totalBidders * 100 : 0;
            
            stats.put("totalBidders", totalBidders);
            stats.put("activeBidders", activeBidders);
            stats.put("totalWinningBids", totalWinningBids);
            stats.put("activeRate", Math.round(activeRate * 100.0) / 100.0);
            
        } catch (Exception e) {
            logger.error("Error getting bidder stats: ", e);
            stats.put("totalBidders", 0);
            stats.put("activeBidders", 0);
            stats.put("totalWinningBids", 0);
            stats.put("activeRate", 0.0);
        }
        
        return stats;
    }

    
    public List<Bidder> searchBidders(String searchTerm, String status, String type) {
        List<Bidder> bidders = bidderRepository.findAll();
        
        return bidders.stream()
            .filter(b -> {
                boolean matchesSearch = searchTerm == null || searchTerm.isEmpty() ||
                    b.getCompanyName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                    b.getEmail().toLowerCase().contains(searchTerm.toLowerCase());
                
                boolean matchesStatus = status == null || status.isEmpty() ||
                    b.getStatus().equalsIgnoreCase(status);
                
                boolean matchesType = type == null || type.isEmpty() ||
                    b.getType().equalsIgnoreCase(type);
                
                return matchesSearch && matchesStatus && matchesType;
            })
            .collect(Collectors.toList());
    }
}

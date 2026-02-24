package com.example.services;

import com.example.dto.BidRequest;
import com.example.dto.BidWithBidderResponse;
import com.example.entity.Bid;
import com.example.entity.Bidder;
import com.example.entity.Tender;
import com.example.repository.BidRepository;
import com.example.repository.BidderRepository;
import com.example.repository.TenderRepository;
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
public class BidService {

    private static final Logger logger = LoggerFactory.getLogger(BidService.class);

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private BidderRepository bidderRepository;

    @Autowired
    private TenderRepository tenderRepository;

    @CacheEvict(value = {"bidsByTender", "bidsByBidder", "bidStats", "tenderBids", "bidsWithTenders", "dashboardData"}, 
               allEntries = true, 
               condition = "#request != null")
    public Map<String, Object> createBid(BidRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            
            Tender tender = tenderRepository.findById(request.getTenderId()).orElse(null);
            if (tender == null) {
                response.put("success", false);
                response.put("message", "Tender not found");
                return response;
            }

            
            if (!"OPEN".equals(tender.getStatus())) {
                response.put("success", false);
                response.put("message", "Tender is not open for bidding");
                return response;
            }

            
            Bidder bidder = bidderRepository.findById(request.getBidderId()).orElse(null);
            if (bidder == null) {
                response.put("success", false);
                response.put("message", "Bidder not found");
                return response;
            }

            
            if (bidRepository.existsByTenderIdAndBidderId(request.getTenderId(), request.getBidderId())) {
                response.put("success", false);
                response.put("message", "You have already placed a bid on this tender");
                return response;
            }

            Bid bid = new Bid();
            bid.setTenderId(request.getTenderId());
            bid.setBidderId(request.getBidderId());
            bid.setBidAmount(request.getBidAmount());
            bid.setProposalText(request.getProposalText());
            bid.setContactNumber(request.getContactNumber());
            bid.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");

            Bid savedBid = bidRepository.save(bid);
            
            
            bidder.setTotalBids(bidder.getTotalBids() + 1);
            bidderRepository.save(bidder);
            
            response.put("success", true);
            response.put("message", "Bid placed successfully");
            response.put("bid", savedBid);
            logger.info("Bid created successfully with ID: {}", savedBid.getId());
            
        } catch (Exception e) {
            logger.error("Error creating bid: ", e);
            response.put("success", false);
            response.put("message", "Error placing bid: " + e.getMessage());
        }
        
        return response;
    }

    
    @Cacheable(value = "bidsByTender", key = "#tenderId", 
              condition = "#tenderId != null",
              unless = "#result == null")
    public List<Bid> getBidsByTender(Long tenderId) {
        logger.debug("Fetching bids for tender: {} (Cache Miss - loading from DB)", tenderId);
        return bidRepository.findByTenderId(tenderId);
    }

   
    @Cacheable(value = "bidsByBidder", key = "#bidderId",
              condition = "#bidderId != null",
              unless = "#result == null")
    public List<Bid> getBidsByBidder(Long bidderId) {
        logger.debug("Fetching bids for bidder: {} (Cache Miss - loading from DB)", bidderId);
        return bidRepository.findByBidderId(bidderId);
    }

    
    @Cacheable(value = "bidsWithTenders", key = "#bidderId",
              condition = "#bidderId != null",
              unless = "#result == null")
    public List<com.example.dto.BidWithTenderResponse> getBidsWithTenderDetails(Long bidderId) {
        logger.debug("Fetching bids with tender details for bidder: {} (Cache Miss - loading from DB)", bidderId);
        List<Bid> bids = bidRepository.findByBidderId(bidderId);
        return bids.stream().map(bid -> {
            com.example.dto.BidWithTenderResponse response = new com.example.dto.BidWithTenderResponse();
            response.setBidId(bid.getId());
            response.setBidderId(bid.getBidderId());
            response.setBidAmount(bid.getBidAmount() != null ? bid.getBidAmount().doubleValue() : null);
            response.setProposalText(bid.getProposalText());
            response.setStatus(bid.getStatus());
            response.setCreatedAt(bid.getCreatedAt() != null ? bid.getCreatedAt().toString() : null);
            response.setContactNumber(bid.getContactNumber());
            response.setHasDocument(bid.getDocumentPath() != null && !bid.getDocumentPath().isEmpty());
            
            
            if (bid.getTenderId() != null) {
                Tender tender = tenderRepository.findById(bid.getTenderId()).orElse(null);
                if (tender != null) {
                    response.setTenderId(tender.getId());
                    response.setTenderName(tender.getName());
                    response.setTenderDescription(tender.getDescription());
                    response.setTenderBudget(tender.getBudget() != null ? tender.getBudget().doubleValue() : null);
                    response.setTenderStatus(tender.getStatus());
                    response.setTenderDeadline(tender.getDeadline() != null ? tender.getDeadline().toString() : null);
                    response.setTenderLocation(tender.getLocation());
                }
            }
            
            return response;
        }).collect(java.util.stream.Collectors.toList());
    }

   
    @Cacheable(value = "bidDetails", key = "#id",
              condition = "#id != null",
              unless = "#result == null")
    public Bid getBidById(Long id) {
        logger.debug("Fetching bid details for ID: {} (Cache Miss - loading from DB)", id);
        return bidRepository.findById(id).orElse(null);
    }

   
    @CacheEvict(value = {"bidDetails", "bidsByTender", "bidsByBidder", "bidStats", "tenderBids", "bidsWithTenders"},
               allEntries = true,
               condition = "#bid != null")
    public Bid saveBid(Bid bid) {
        logger.debug("Saving bid - evicting all related caches");
        return bidRepository.save(bid);
    }

    
    @CacheEvict(value = {"bidDetails", "bidsByTender", "bidsByBidder", "bidStats", "tenderBids", "bidsWithTenders", "dashboardData"},
               allEntries = true,
               condition = "#bidId != null")
    public Map<String, Object> updateBidStatus(Long bidId, String status) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Bid bid = bidRepository.findById(bidId).orElse(null);
            if (bid == null) {
                response.put("success", false);
                response.put("message", "Bid not found");
                return response;
            }

            
            if ("WINNING".equals(status)) {
                List<Bid> allBids = bidRepository.findByTenderId(bid.getTenderId());
                for (Bid b : allBids) {
                    if (b.getIsWinning()) {
                        b.setIsWinning(false);
                        b.setStatus("REJECTED");
                        bidRepository.save(b);
                    }
                }
                
                
                Bidder bidder = bidderRepository.findById(bid.getBidderId()).orElse(null);
                if (bidder != null) {
                    bidder.setWinningBids(bidder.getWinningBids() + 1);
                    bidderRepository.save(bidder);
                }
            }

            
            if ("APPROVED".equals(status) || "ACCEPTED".equals(status) || "WINNING".equals(status)) {
                List<Bid> allBidsForTender = bidRepository.findByTenderId(bid.getTenderId());
                for (Bid otherBid : allBidsForTender) {
                    
                    if (!otherBid.getId().equals(bidId)) {
                        
                        if ("PENDING".equals(otherBid.getStatus())) {
                            otherBid.setStatus("REJECTED");
                            bidRepository.save(otherBid);
                            logger.info("Auto-rejected bid ID: {} for tender ID: {} as another bid was approved", 
                                otherBid.getId(), bid.getTenderId());
                        }
                    }
                }
            }

            bid.setStatus(status);
            bid.setIsWinning("WINNING".equals(status));
            
            Bid updatedBid = bidRepository.save(bid);
            
            response.put("success", true);
            response.put("message", "Bid status updated successfully");
            response.put("bid", updatedBid);
            logger.info("Bid status updated successfully for ID: {}", bidId);
            
        } catch (Exception e) {
            logger.error("Error updating bid status: ", e);
            response.put("success", false);
            response.put("message", "Error updating bid status: " + e.getMessage());
        }
        
        return response;
    }

   
    @CacheEvict(value = {"bidDetails", "bidsByTender", "bidsByBidder", "bidStats", "tenderBids", "bidsWithTenders", "dashboardData"},
               allEntries = true,
               condition = "#bidId != null")
    public Map<String, Object> deleteBid(Long bidId, Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Bid bid = bidRepository.findById(bidId).orElse(null);
            if (bid == null) {
                response.put("success", false);
                response.put("message", "Bid not found");
                return response;
            }

            
            Bidder bidder = bidderRepository.findById(bid.getBidderId()).orElse(null);
            if (bidder != null) {
                bidder.setTotalBids(Math.max(0, bidder.getTotalBids() - 1));
                if (bid.getIsWinning()) {
                    bidder.setWinningBids(Math.max(0, bidder.getWinningBids() - 1));
                }
                bidderRepository.save(bidder);
            }

            bidRepository.delete(bid);
            
            response.put("success", true);
            response.put("message", "Bid deleted successfully");
            logger.info("Bid deleted successfully with ID: {}", bidId);
            
        } catch (Exception e) {
            logger.error("Error deleting bid: ", e);
            response.put("success", false);
            response.put("message", "Error deleting bid: " + e.getMessage());
        }
        
        return response;
    }

   
    @Cacheable(value = "bidStats", key = "#tenderId",
              condition = "#tenderId != null",
              unless = "#result == null")
    public Map<String, Object> getBidStats(Long tenderId) {
        logger.debug("Fetching bid stats for tender: {} (Cache Miss - loading from DB)", tenderId);
        Map<String, Object> stats = new HashMap<>();
        
        try {
            List<Bid> bids = bidRepository.findByTenderId(tenderId);
            
            int totalBids = bids.size();
            long pendingBids = bids.stream().filter(b -> "PENDING".equals(b.getStatus())).count();
            long acceptedBids = bids.stream().filter(b -> "ACCEPTED".equals(b.getStatus())).count();
            long rejectedBids = bids.stream().filter(b -> "REJECTED".equals(b.getStatus())).count();
            
            stats.put("totalBids", totalBids);
            stats.put("pendingBids", pendingBids);
            stats.put("acceptedBids", acceptedBids);
            stats.put("rejectedBids", rejectedBids);
            
        } catch (Exception e) {
            logger.error("Error getting bid stats: ", e);
            stats.put("totalBids", 0);
            stats.put("pendingBids", 0);
            stats.put("acceptedBids", 0);
            stats.put("rejectedBids", 0);
        }
        
        return stats;
    }
    
  
    @Cacheable(value = "tenderBids", key = "#tenderId",
              condition = "#tenderId != null",
              unless = "#result == null")
    public List<BidWithBidderResponse> getBidsWithBidderDetails(Long tenderId) {
        logger.debug("Fetching bids with bidder details for tender: {} (Cache Miss - loading from DB)", tenderId);
        try {
            List<Bid> bids = bidRepository.findByTenderId(tenderId);
            
            return bids.stream().map(bid -> {
                BidWithBidderResponse response = new BidWithBidderResponse();
                response.setBidId(bid.getId());
                response.setTenderId(bid.getTenderId());
                response.setBidderId(bid.getBidderId());
                response.setBidAmount(bid.getBidAmount());
                response.setProposalText(bid.getProposalText());
                response.setStatus(bid.getStatus());
                response.setIsWinning(bid.getIsWinning());
                response.setContactNumber(bid.getContactNumber());
                response.setCreatedAt(bid.getCreatedAt());
                response.setUpdatedAt(bid.getUpdatedAt());
                response.setDocumentPath(bid.getDocumentPath());
                response.setDocumentPaths(bid.getDocumentPaths());
                
                
                Bidder bidder = bidderRepository.findById(bid.getBidderId()).orElse(null);
                if (bidder != null) {
                    response.setBidderCompanyName(bidder.getCompanyName());
                    response.setBidderEmail(bidder.getEmail());
                    response.setBidderPhone(bidder.getPhone());
                    response.setBidderType(bidder.getType());
                    response.setBidderContactPerson(bidder.getContactPerson());
                }
                
                return response;
            }).collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Error getting bids with bidder details: ", e);
            return new java.util.ArrayList<>();
        }
    }
}

package com.example.services;

import com.example.dto.DashboardResponse;
import com.example.entity.Bid;
import com.example.entity.Bidder;
import com.example.entity.Tender;
import com.example.repository.BidRepository;
import com.example.repository.BidderRepository;
import com.example.repository.TenderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Autowired
    private TenderRepository tenderRepository;
    
    @Autowired
    private BidRepository bidRepository;
    
    @Autowired
    private BidderRepository bidderRepository;

    
    public DashboardResponse getDashboardData(String username) {
        logger.debug("Fetching dashboard data for user: {} (Cache Miss - loading from DB)", username);
        
        try {
            // Update tender statuses based on deadline before counting
            updateTenderStatusesForDashboard();
            
            // Get stats
            long totalTenders = tenderRepository.count();
            long openTenders = 0;
            long closedTenders = 0;
            
            // Get tender status counts
            List<Object[]> tenderStatusGrouped = tenderRepository.countByStatusGrouped();
            for (Object[] obj : tenderStatusGrouped) {
                String status = (String) obj[0];
                Long count = (Long) obj[1];
                if ("OPEN".equals(status)) {
                    openTenders = count;
                } else if ("CLOSED".equals(status)) {
                    closedTenders = count;
                }
            }
            
            long totalBids = bidRepository.countTotalBids();
            long pendingBids = 0;
            long approvedBids = 0;
            long rejectedBids = 0;
            
            // Get bid status counts
            List<Object[]> bidStatusGrouped = bidRepository.countByStatusGrouped();
            for (Object[] obj : bidStatusGrouped) {
                String status = (String) obj[0];
                Long count = (Long) obj[1];
                if ("PENDING".equals(status)) {
                    pendingBids = count;
                } else if ("APPROVED".equals(status) || "WINNING".equals(status)) {
                    approvedBids += count;
                } else if ("REJECTED".equals(status)) {
                    rejectedBids = count;
                }
            }
            
            long totalBidders = bidderRepository.countTotalBidders();
            long activeBidders = 0;
            long inactiveBidders = 0;
            
            // Get bidder status counts
            List<Object[]> bidderStatusGrouped = bidderRepository.countByStatusGrouped();
            for (Object[] obj : bidderStatusGrouped) {
                String status = (String) obj[0];
                Long count = (Long) obj[1];
                if ("ACTIVE".equals(status)) {
                    activeBidders = count;
                } else if ("INACTIVE".equals(status)) {
                    inactiveBidders = count;
                }
            }
            
            // Get tender status data for chart
            List<DashboardResponse.StatusCount> tenderStatusData = new ArrayList<>();
            for (Object[] obj : tenderStatusGrouped) {
                tenderStatusData.add(new DashboardResponse.StatusCount(
                    (String) obj[0],
                    (Long) obj[1]
                ));
            }
            
            // Get bid status data for chart
            List<DashboardResponse.StatusCount> bidStatusData = new ArrayList<>();
            for (Object[] obj : bidStatusGrouped) {
                bidStatusData.add(new DashboardResponse.StatusCount(
                    (String) obj[0],
                    (Long) obj[1]
                ));
            }
            
            // Get bidder status data for chart
            List<DashboardResponse.StatusCount> bidderStatusData = new ArrayList<>();
            for (Object[] obj : bidderStatusGrouped) {
                bidderStatusData.add(new DashboardResponse.StatusCount(
                    (String) obj[0],
                    (Long) obj[1]
                ));
            }
            
            // Get monthly tenders data (last 6 months) - sample data, can be enhanced with actual queries
            List<DashboardResponse.MonthlyData> monthlyTenders = new ArrayList<>();
            if (totalTenders > 0) {
                monthlyTenders.add(new DashboardResponse.MonthlyData("Jan", Math.max(1, totalTenders / 6)));
                monthlyTenders.add(new DashboardResponse.MonthlyData("Feb", Math.max(1, totalTenders / 5)));
                monthlyTenders.add(new DashboardResponse.MonthlyData("Mar", Math.max(1, totalTenders / 4)));
                monthlyTenders.add(new DashboardResponse.MonthlyData("Apr", Math.max(1, totalTenders / 3)));
                monthlyTenders.add(new DashboardResponse.MonthlyData("May", Math.max(1, totalTenders / 2)));
                monthlyTenders.add(new DashboardResponse.MonthlyData("Jun", totalTenders));
            } else {
                monthlyTenders.add(new DashboardResponse.MonthlyData("Jan", 0));
                monthlyTenders.add(new DashboardResponse.MonthlyData("Feb", 0));
                monthlyTenders.add(new DashboardResponse.MonthlyData("Mar", 0));
                monthlyTenders.add(new DashboardResponse.MonthlyData("Apr", 0));
                monthlyTenders.add(new DashboardResponse.MonthlyData("May", 0));
                monthlyTenders.add(new DashboardResponse.MonthlyData("Jun", 0));
            }
            
            // Get monthly bids data
            List<DashboardResponse.MonthlyData> monthlyBids = new ArrayList<>();
            if (totalBids > 0) {
                monthlyBids.add(new DashboardResponse.MonthlyData("Jan", Math.max(1, totalBids / 6)));
                monthlyBids.add(new DashboardResponse.MonthlyData("Feb", Math.max(1, totalBids / 5)));
                monthlyBids.add(new DashboardResponse.MonthlyData("Mar", Math.max(1, totalBids / 4)));
                monthlyBids.add(new DashboardResponse.MonthlyData("Apr", Math.max(1, totalBids / 3)));
                monthlyBids.add(new DashboardResponse.MonthlyData("May", Math.max(1, totalBids / 2)));
                monthlyBids.add(new DashboardResponse.MonthlyData("Jun", totalBids));
            } else {
                monthlyBids.add(new DashboardResponse.MonthlyData("Jan", 0));
                monthlyBids.add(new DashboardResponse.MonthlyData("Feb", 0));
                monthlyBids.add(new DashboardResponse.MonthlyData("Mar", 0));
                monthlyBids.add(new DashboardResponse.MonthlyData("Apr", 0));
                monthlyBids.add(new DashboardResponse.MonthlyData("May", 0));
                monthlyBids.add(new DashboardResponse.MonthlyData("Jun", 0));
            }
            
            // Get recent tenders
            List<DashboardResponse.TenderSummary> recentTenders = new ArrayList<>();
            List<Tender> recentTenderList = tenderRepository.findRecentTenders(PageRequest.of(0, 5));
            for (Tender tender : recentTenderList) {
                recentTenders.add(new DashboardResponse.TenderSummary(
                    tender.getId(),
                    tender.getName(),
                    tender.getStatus(),
                    tender.getBudget() != null ? tender.getBudget().toString() : "N/A",
                    tender.getCreatedAt() != null ? tender.getCreatedAt().format(DATE_FORMATTER) : "N/A"
                ));
            }
            
            // Get recent bids
            List<DashboardResponse.BidSummary> recentBids = new ArrayList<>();
            List<Bid> recentBidList = bidRepository.findRecentBids(PageRequest.of(0, 5));
            for (Bid bid : recentBidList) {
                String tenderName = "Tender #" + bid.getTenderId();
                try {
                    Optional<Tender> tenderOpt = tenderRepository.findById(bid.getTenderId());
                    if (tenderOpt.isPresent()) {
                        tenderName = tenderOpt.get().getName();
                    }
                } catch (Exception e) {
                    logger.warn("Could not fetch tender name for tenderId: {}", bid.getTenderId());
                }
                recentBids.add(new DashboardResponse.BidSummary(
                    bid.getId(),
                    bid.getTenderId(),
                    tenderName,
                    bid.getBidAmount() != null ? bid.getBidAmount().toString() : "N/A",
                    bid.getStatus(),
                    bid.getCreatedAt() != null ? bid.getCreatedAt().format(DATE_FORMATTER) : "N/A"
                ));
            }
            
            // Get recent bidders
            List<DashboardResponse.BidderSummary> recentBidders = new ArrayList<>();
            List<Bidder> recentBidderList = bidderRepository.findRecentBidders(PageRequest.of(0, 5));
            for (Bidder bidder : recentBidderList) {
                recentBidders.add(new DashboardResponse.BidderSummary(
                    bidder.getId(),
                    bidder.getCompanyName(),
                    bidder.getEmail(),
                    bidder.getStatus(),
                    bidder.getTotalBids() != null ? bidder.getTotalBids() : 0,
                    bidder.getCreatedAt() != null ? bidder.getCreatedAt().format(DATE_FORMATTER) : "N/A"
                ));
            }
            
            return new DashboardResponse(
                "Dashboard loaded successfully",
                username,
                totalTenders,
                openTenders,
                closedTenders,
                totalBids,
                pendingBids,
                approvedBids,
                rejectedBids,
                totalBidders,
                activeBidders,
                inactiveBidders,
                tenderStatusData,
                bidStatusData,
                bidderStatusData,
                monthlyTenders,
                monthlyBids,
                recentTenders,
                recentBids,
                recentBidders
            );
        } catch (Exception e) {
            logger.error("Error loading dashboard data: ", e);
            // Return empty response on error
            return new DashboardResponse(
                "Error loading dashboard",
                username != null ? username : "Unknown",
                0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>()
            );
        }
    }
    
    // Update tender statuses based on deadline before getting counts
    private void updateTenderStatusesForDashboard() {
        LocalDateTime now = LocalDateTime.now();
        List<Tender> expiredTenders = tenderRepository.findOpenTendersWithExpiredDeadline(now);
        
        if (!expiredTenders.isEmpty()) {
            logger.info("Dashboard: Updating {} expired tenders to CLOSED", expiredTenders.size());
            for (Tender tender : expiredTenders) {
                tender.setStatus("CLOSED");
                tenderRepository.save(tender);
            }
            // Evict dashboard cache after updating statuses
            evictDashboardCache();
        }
    }
    
    // Public method to evict dashboard cache (can be called from other services)
    @org.springframework.cache.annotation.CacheEvict(value = "dashboardData", allEntries = true)
    public void evictDashboardCache() {
        logger.info("Dashboard cache evicted");
    }
}

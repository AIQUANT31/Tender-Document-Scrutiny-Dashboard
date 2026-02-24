package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private String message;
    private String username;
    
    
    private long totalTenders;
    private long openTenders;
    private long closedTenders;
    private long totalBids;
    private long pendingBids;
    private long approvedBids;
    private long rejectedBids;
    private long totalBidders;
    private long activeBidders;
    private long inactiveBidders;
    
    
    private List<StatusCount> tenderStatusData;
    private List<StatusCount> bidStatusData;
    private List<StatusCount> bidderStatusData;
    private List<MonthlyData> monthlyTenders;
    private List<MonthlyData> monthlyBids;
    
    
    private List<TenderSummary> recentTenders;
    private List<BidSummary> recentBids;
    private List<BidderSummary> recentBidders;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusCount {
        private String status;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyData {
        private String month;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenderSummary {
        private Long id;
        private String name;
        private String status;
        private String budget;
        private String createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BidSummary {
        private Long id;
        private Long tenderId;
        private String tenderName;
        private String bidAmount;
        private String status;
        private String createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BidderSummary {
        private Long id;
        private String companyName;
        private String email;
        private String status;
        private int totalBids;
        private String createdAt;
    }
}

package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidWithBidderResponse {
    private Long bidId;
    private Long tenderId;
    private Long bidderId;
    private String bidderCompanyName;
    private String bidderEmail;
    private String bidderPhone;
    private String bidderType;
    private String bidderContactPerson;
    private BigDecimal bidAmount;
    private String proposalText;
    private String status;
    private Boolean isWinning;
    private String contactNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String documentPath;
    private String documentPaths;
}

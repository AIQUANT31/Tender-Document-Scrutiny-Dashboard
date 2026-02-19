package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidRequest {
    private Long tenderId;
    private Long bidderId;
    private BigDecimal bidAmount;
    private String proposalText;
    private String contactNumber;
    private String status;
}

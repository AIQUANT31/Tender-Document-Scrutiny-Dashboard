package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BidderRequest {
    private String companyName;
    private String email;
    private String phone;
    private String type;
    private Long createdBy;
    private String address;
    private String contactPerson;
    private Integer totalBids;
    private Integer winningBids;
    private String status;
}

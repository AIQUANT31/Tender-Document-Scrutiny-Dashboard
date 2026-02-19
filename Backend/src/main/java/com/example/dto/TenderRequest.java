package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenderRequest {
    private String name;
    private String description;
    private Long createdBy;
    private java.math.BigDecimal budget;
    private java.time.LocalDateTime deadline;
    private java.time.LocalDateTime openingDate;
    private String requiredDocuments;
    private String category;
    private String location;
    private String contactNumber;
    private Boolean isWhatsapp;
    private String comments;
    private String userType;
}

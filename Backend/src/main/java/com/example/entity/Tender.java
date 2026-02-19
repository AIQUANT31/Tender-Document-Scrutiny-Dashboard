package com.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(length = 50)
    private String status;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(precision = 12, scale = 2)
    private java.math.BigDecimal budget;

    @Column(name = "required_documents", columnDefinition = "TEXT")
    private String requiredDocuments;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "opening_date")
    private LocalDateTime openingDate;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(name = "is_whatsapp")
    private Boolean isWhatsapp = false;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "user_type", length = 50)
    private String userType;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "OPEN";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

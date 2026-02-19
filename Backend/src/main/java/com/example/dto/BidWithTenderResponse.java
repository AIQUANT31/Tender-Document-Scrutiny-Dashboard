package com.example.dto;

public class BidWithTenderResponse {
    private Long bidId;
    private Long tenderId;
    private String tenderName;
    private String tenderDescription;
    private Double tenderBudget;
    private String tenderStatus;
    private String tenderDeadline;
    private String tenderLocation;
    private Long bidderId;
    private Double bidAmount;
    private String proposalText;
    private String status;
    private String createdAt;
    private String contactNumber;
    private Boolean hasDocument;

    // Getters and Setters
    public Long getBidId() {
        return bidId;
    }

    public void setBidId(Long bidId) {
        this.bidId = bidId;
    }

    public Long getTenderId() {
        return tenderId;
    }

    public void setTenderId(Long tenderId) {
        this.tenderId = tenderId;
    }

    public String getTenderName() {
        return tenderName;
    }

    public void setTenderName(String tenderName) {
        this.tenderName = tenderName;
    }

    public String getTenderDescription() {
        return tenderDescription;
    }

    public void setTenderDescription(String tenderDescription) {
        this.tenderDescription = tenderDescription;
    }

    public Double getTenderBudget() {
        return tenderBudget;
    }

    public void setTenderBudget(Double tenderBudget) {
        this.tenderBudget = tenderBudget;
    }

    public String getTenderStatus() {
        return tenderStatus;
    }

    public void setTenderStatus(String tenderStatus) {
        this.tenderStatus = tenderStatus;
    }

    public String getTenderDeadline() {
        return tenderDeadline;
    }

    public void setTenderDeadline(String tenderDeadline) {
        this.tenderDeadline = tenderDeadline;
    }

    public String getTenderLocation() {
        return tenderLocation;
    }

    public void setTenderLocation(String tenderLocation) {
        this.tenderLocation = tenderLocation;
    }

    public Long getBidderId() {
        return bidderId;
    }

    public void setBidderId(Long bidderId) {
        this.bidderId = bidderId;
    }

    public Double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(Double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public String getProposalText() {
        return proposalText;
    }

    public void setProposalText(String proposalText) {
        this.proposalText = proposalText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public Boolean getHasDocument() {
        return hasDocument;
    }

    public void setHasDocument(Boolean hasDocument) {
        this.hasDocument = hasDocument;
    }
}

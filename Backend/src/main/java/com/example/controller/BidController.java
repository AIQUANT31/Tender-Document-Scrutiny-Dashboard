package com.example.controller;

import com.example.dto.BidRequest;
import com.example.dto.BidWithBidderResponse;
import com.example.entity.Bid;
import com.example.services.BidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bids")
@CrossOrigin(origins = "*")
public class BidController {

    @Autowired
    private BidService bidService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createBid(@RequestBody BidRequest request) {
        Map<String, Object> response = bidService.createBid(request);
        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Get all bids for a specific tender
    @GetMapping("/tender/{tenderId}")
    public ResponseEntity<List<Bid>> getBidsByTender(@PathVariable Long tenderId) {
        List<Bid> bids = bidService.getBidsByTender(tenderId);
        return ResponseEntity.ok(bids);
    }

    //  Get all bids from a specific bidder
    @GetMapping("/bidder/{bidderId}")
    public ResponseEntity<List<Bid>> getBidsByBidder(@PathVariable Long bidderId) {
        List<Bid> bids = bidService.getBidsByBidder(bidderId);
        return ResponseEntity.ok(bids);
    }

     //    Get bids with tender details
    @GetMapping("/bidder/{bidderId}/with-tenders")
    public ResponseEntity<List<com.example.dto.BidWithTenderResponse>> getBidsWithTenderDetails(
            @PathVariable Long bidderId) {
        List<com.example.dto.BidWithTenderResponse> bids = bidService.getBidsWithTenderDetails(bidderId);
        return ResponseEntity.ok(bids);
    }

//   Get a specific bid by ID
    @GetMapping("/{id}")
    public ResponseEntity<Bid> getBidById(@PathVariable Long id) {
        Bid bid = bidService.getBidById(id);
        if (bid != null) {
            return ResponseEntity.ok(bid);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Get bid statistics for a tender
    @GetMapping("/tender/{tenderId}/stats")
    public ResponseEntity<Map<String, Object>> getBidStats(@PathVariable Long tenderId) {
        Map<String, Object> stats = bidService.getBidStats(tenderId);
        return ResponseEntity.ok(stats);
    }

//  Get bids with bidder details for a tender
    @GetMapping("/tender/{tenderId}/with-bidders")
    public ResponseEntity<List<BidWithBidderResponse>> getBidsWithBidderDetails(@PathVariable Long tenderId) {
        List<BidWithBidderResponse> bids = bidService.getBidsWithBidderDetails(tenderId);
        return ResponseEntity.ok(bids);
    }

//    update bid status
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateBidStatus(
            @PathVariable Long id, 
            @RequestParam String status) {
        Map<String, Object> response = bidService.updateBidStatus(id, status);
        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

//    delete bid 
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBid(
            @PathVariable Long id, 
            @RequestParam Long userId) {
        Map<String, Object> response = bidService.deleteBid(id, userId);
        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}

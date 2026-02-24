package com.example.controller;

import com.example.dto.BidderRequest;
import com.example.entity.Bidder;
import com.example.services.BidderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bidders")
public class BidderController {

    @Autowired
    private BidderService bidderService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createBidder(@RequestBody BidderRequest request) {
        Map<String, Object> response = bidderService.createBidder(request);
        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<List<Bidder>> getBidders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        
        List<Bidder> bidders;
        if (search != null || status != null || type != null) {
            bidders = bidderService.searchBidders(search, status, type);
        } else {
            bidders = bidderService.getAllBidders();
        }
        return ResponseEntity.ok(bidders);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Bidder>> getBiddersByUser(@PathVariable Long userId) {
        List<Bidder> bidders = bidderService.getBiddersByUser(userId);
        return ResponseEntity.ok(bidders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bidder> getBidderById(@PathVariable Long id) {
        Bidder bidder = bidderService.getBidderById(id);
        if (bidder != null) {
            return ResponseEntity.ok(bidder);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getBidderStats() {
        Map<String, Object> stats = bidderService.getBidderStats();
        return ResponseEntity.ok(stats);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateBidder(
            @PathVariable Long id, 
            @RequestParam Long userId,
            @RequestBody BidderRequest request) {
        Map<String, Object> response = bidderService.updateBidder(id, userId, request);
        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBidder(
            @PathVariable Long id, 
            @RequestParam Long userId) {
        Map<String, Object> response = bidderService.deleteBidder(id, userId);
        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }
}

package com.example.controller;

import com.example.dto.TenderRequest;
import com.example.entity.Tender;
import com.example.services.TenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenders")
public class TenderController {

    @Autowired
    private TenderService tenderService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createTender(@RequestBody TenderRequest request) {
        Map<String, Object> response = tenderService.createTender(request);
        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping
    public ResponseEntity<List<Tender>> getTenders() {
        List<Tender> tenders = tenderService.getAllTenders();
        return ResponseEntity.ok(tenders);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Tender>> getTendersByUser(@PathVariable Long userId) {
        List<Tender> tenders = tenderService.getTendersByUser(userId);
        return ResponseEntity.ok(tenders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tender> getTenderById(@PathVariable Long id) {
        Tender tender = tenderService.getTenderById(id);
        if (tender != null) {
            return ResponseEntity.ok(tender);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTender(@PathVariable Long id, @RequestParam Long userId) {
        Map<String, Object> response = tenderService.deleteTender(id, userId);
        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }

    @PutMapping("/{id}/comments")
    public ResponseEntity<Map<String, Object>> updateComments(
            @PathVariable Long id, 
            @RequestParam Long userId,
            @RequestBody Map<String, String> request) {
        String comments = request.get("comments");
        Map<String, Object> response = tenderService.updateComments(id, userId, comments);
        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateTenderStatus(
            @PathVariable Long id, 
            @RequestParam String status) {
        Map<String, Object> response = tenderService.updateTenderStatus(id, status);
        if (response.containsKey("success") && (Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}

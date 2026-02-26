package com.example.controller;

import com.example.dto.DashboardResponse;
import com.example.services.dashboard.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/data")
    public ResponseEntity<DashboardResponse> getDashboardData(
            @RequestParam(required = false, defaultValue = "User") String username) {
        DashboardResponse response = dashboardService.getDashboardData(username);
        return ResponseEntity.ok(response);
        
    }
}

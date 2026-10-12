package com.textile.accounts.dashboard.controller;

import com.textile.accounts.dashboard.dto.DashboardDtos.DashboardSummary;
import com.textile.accounts.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardSummary> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }
}

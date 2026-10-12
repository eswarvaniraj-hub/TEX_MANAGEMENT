package com.textile.accounts.reports.controller;

import com.textile.accounts.reports.dto.ReportsDtos.DayBookLine;
import com.textile.accounts.reports.dto.ReportsDtos.TrialBalanceReport;
import com.textile.accounts.reports.service.ReportsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportsController {

    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping("/trial-balance")
    public ResponseEntity<TrialBalanceReport> getTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate
    ) {
        return ResponseEntity.ok(reportsService.getTrialBalance(asOfDate));
    }

    @GetMapping("/day-book")
    public ResponseEntity<List<DayBookLine>> getDayBook(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(reportsService.getDayBook(date));
    }
}

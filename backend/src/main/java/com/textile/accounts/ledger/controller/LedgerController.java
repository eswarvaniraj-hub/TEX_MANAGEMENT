package com.textile.accounts.ledger.controller;

import com.textile.accounts.ledger.dto.LedgerDtos.JournalEntryResponse;
import com.textile.accounts.ledger.dto.LedgerDtos.PartyLedgerLine;
import com.textile.accounts.ledger.service.IntegrityCheckResult;
import com.textile.accounts.ledger.service.LedgerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/entries")
    public ResponseEntity<Page<JournalEntryResponse>> searchEntries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String sourceType,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ledgerService.searchEntries(from, to, sourceType, pageable));
    }

    @GetMapping("/entries/{id}")
    public ResponseEntity<JournalEntryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ledgerService.getById(id));
    }

    @GetMapping("/customers/{id}/lines")
    public ResponseEntity<List<PartyLedgerLine>> getCustomerLedger(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(ledgerService.getCustomerLedger(id, from, to));
    }

    @GetMapping("/suppliers/{id}/lines")
    public ResponseEntity<List<PartyLedgerLine>> getSupplierLedger(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(ledgerService.getSupplierLedger(id, from, to));
    }

    @GetMapping("/integrity-check")
    public ResponseEntity<IntegrityCheckResult> checkIntegrity() {
        return ResponseEntity.ok(ledgerService.checkIntegrity());
    }
}

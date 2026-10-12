package com.textile.accounts.banking.controller;

import com.textile.accounts.banking.dto.BankingDtos.AccountStatementLine;
import com.textile.accounts.banking.dto.BankingDtos.FundTransferRequest;
import com.textile.accounts.banking.dto.BankingDtos.FundTransferResponse;
import com.textile.accounts.banking.dto.BankingDtos.ReverseTransferRequest;
import com.textile.accounts.banking.service.BankingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/banking")
public class BankingController {

    private final BankingService bankingService;

    public BankingController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @GetMapping("/transfers")
    public ResponseEntity<Page<FundTransferResponse>> searchTransfers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable
    ) {
        return ResponseEntity.ok(bankingService.searchTransfers(from, to, pageable));
    }

    @PostMapping("/transfers")
    public ResponseEntity<FundTransferResponse> transferFunds(
            @Valid @RequestBody FundTransferRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(bankingService.transferFunds(request, actor));
    }

    @PostMapping("/transfers/{id}/reverse")
    public ResponseEntity<FundTransferResponse> reverseTransfer(
            @PathVariable Long id,
            @RequestBody(required = false) ReverseTransferRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(bankingService.reverseTransfer(id, request, actor));
    }

    @GetMapping("/accounts/{id}/statement")
    public ResponseEntity<List<AccountStatementLine>> getAccountStatement(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(bankingService.getAccountStatement(id, from, to));
    }
}

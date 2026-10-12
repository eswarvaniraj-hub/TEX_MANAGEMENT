package com.textile.accounts.sales.controller;

import com.textile.accounts.sales.dto.SalesDtos.CalculationResponse;
import com.textile.accounts.sales.dto.SalesDtos.ReverseRequest;
import com.textile.accounts.sales.dto.SalesDtos.SalesInvoiceRequest;
import com.textile.accounts.sales.dto.SalesDtos.SalesInvoiceResponse;
import com.textile.accounts.sales.service.SalesInvoiceCalculationService;
import com.textile.accounts.sales.service.SalesInvoiceService;
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

@RestController
@RequestMapping("/api/v1/sales-invoices")
public class SalesInvoiceController {

    private final SalesInvoiceService salesInvoiceService;
    private final SalesInvoiceCalculationService calculationService;

    public SalesInvoiceController(
            SalesInvoiceService salesInvoiceService,
            SalesInvoiceCalculationService calculationService
    ) {
        this.salesInvoiceService = salesInvoiceService;
        this.calculationService = calculationService;
    }

    @GetMapping
    public ResponseEntity<Page<SalesInvoiceResponse>> search(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            Pageable pageable
    ) {
        return ResponseEntity.ok(salesInvoiceService.search(customerId, status, from, to, q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesInvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(salesInvoiceService.getById(id));
    }

    @PostMapping("/calculate")
    public ResponseEntity<CalculationResponse> calculate(@Valid @RequestBody SalesInvoiceRequest request) {
        return ResponseEntity.ok(calculationService.calculate(request));
    }

    @PostMapping
    public ResponseEntity<SalesInvoiceResponse> createDraft(
            @Valid @RequestBody SalesInvoiceRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(salesInvoiceService.createDraft(request, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalesInvoiceResponse> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody SalesInvoiceRequest request,
            @RequestParam(required = false) Long version,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(salesInvoiceService.updateDraft(id, request, version, actor));
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<SalesInvoiceResponse> postInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(salesInvoiceService.postInvoice(id, actor));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<SalesInvoiceResponse> reverseInvoice(
            @PathVariable Long id,
            @RequestBody(required = false) ReverseRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(salesInvoiceService.reverseInvoice(id, request, actor));
    }
}

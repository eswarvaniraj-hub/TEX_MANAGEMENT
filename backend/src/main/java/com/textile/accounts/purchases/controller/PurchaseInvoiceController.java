package com.textile.accounts.purchases.controller;

import com.textile.accounts.purchases.dto.PurchaseDtos.PurchaseCalculationResponse;
import com.textile.accounts.purchases.dto.PurchaseDtos.PurchaseInvoiceRequest;
import com.textile.accounts.purchases.dto.PurchaseDtos.PurchaseInvoiceResponse;
import com.textile.accounts.purchases.dto.PurchaseDtos.ReversePurchaseRequest;
import com.textile.accounts.purchases.service.PurchaseInvoiceService;
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
@RequestMapping("/api/v1/purchase-invoices")
public class PurchaseInvoiceController {

    private final PurchaseInvoiceService purchaseInvoiceService;

    public PurchaseInvoiceController(PurchaseInvoiceService purchaseInvoiceService) {
        this.purchaseInvoiceService = purchaseInvoiceService;
    }

    @GetMapping
    public ResponseEntity<Page<PurchaseInvoiceResponse>> search(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            Pageable pageable
    ) {
        return ResponseEntity.ok(purchaseInvoiceService.search(supplierId, status, from, to, q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseInvoiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseInvoiceService.getById(id));
    }

    @PostMapping("/calculate")
    public ResponseEntity<PurchaseCalculationResponse> calculate(@Valid @RequestBody PurchaseInvoiceRequest request) {
        return ResponseEntity.ok(purchaseInvoiceService.calculate(request));
    }

    @PostMapping
    public ResponseEntity<PurchaseInvoiceResponse> createDraft(
            @Valid @RequestBody PurchaseInvoiceRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseInvoiceService.createDraft(request, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PurchaseInvoiceResponse> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseInvoiceRequest request,
            @RequestParam(required = false) Long version,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(purchaseInvoiceService.updateDraft(id, request, version, actor));
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<PurchaseInvoiceResponse> postInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(purchaseInvoiceService.postInvoice(id, actor));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<PurchaseInvoiceResponse> reverseInvoice(
            @PathVariable Long id,
            @RequestBody(required = false) ReversePurchaseRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(purchaseInvoiceService.reverseInvoice(id, request, actor));
    }
}

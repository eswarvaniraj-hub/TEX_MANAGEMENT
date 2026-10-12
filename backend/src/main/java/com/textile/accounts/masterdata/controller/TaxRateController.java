package com.textile.accounts.masterdata.controller;

import com.textile.accounts.masterdata.dto.MasterDataDtos.TaxRateRequest;
import com.textile.accounts.masterdata.dto.MasterDataDtos.TaxRateResponse;
import com.textile.accounts.masterdata.service.TaxRateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tax-rates")
public class TaxRateController {

    private final TaxRateService taxRateService;

    public TaxRateController(TaxRateService taxRateService) {
        this.taxRateService = taxRateService;
    }

    @GetMapping
    public ResponseEntity<List<TaxRateResponse>> getAllTaxRates(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(taxRateService.getAllTaxRates(active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaxRateResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(taxRateService.getById(id));
    }

    @PostMapping
    public ResponseEntity<TaxRateResponse> create(
            @Valid @RequestBody TaxRateRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(taxRateService.createTaxRate(request, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaxRateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TaxRateRequest request,
            @RequestParam(required = false) Long version,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(taxRateService.updateTaxRate(id, request, version, actor));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<TaxRateResponse> setActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(taxRateService.setTaxRateActive(id, active, actor));
    }
}

package com.textile.accounts.masterdata.controller;

import com.textile.accounts.masterdata.dto.MasterDataDtos.SupplierRequest;
import com.textile.accounts.masterdata.dto.MasterDataDtos.SupplierResponse;
import com.textile.accounts.masterdata.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public ResponseEntity<Page<SupplierResponse>> searchSuppliers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            Pageable pageable
    ) {
        return ResponseEntity.ok(supplierService.searchSuppliers(q, active, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(
            @Valid @RequestBody SupplierRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.createSupplier(request, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request,
            @RequestParam(required = false) Long version,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(supplierService.updateSupplier(id, request, version, actor));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<SupplierResponse> setActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(supplierService.setSupplierActive(id, active, actor));
    }
}

package com.textile.accounts.masterdata.controller;

import com.textile.accounts.masterdata.dto.MasterDataDtos.CustomerRequest;
import com.textile.accounts.masterdata.dto.MasterDataDtos.CustomerResponse;
import com.textile.accounts.masterdata.service.CustomerService;
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
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> searchCustomers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            Pageable pageable
    ) {
        return ResponseEntity.ok(customerService.searchCustomers(q, active, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(
            @Valid @RequestBody CustomerRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request,
            @RequestParam(required = false) Long version,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(customerService.updateCustomer(id, request, version, actor));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<CustomerResponse> setActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(customerService.setCustomerActive(id, active, actor));
    }
}

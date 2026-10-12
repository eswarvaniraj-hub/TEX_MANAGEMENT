package com.textile.accounts.banking.controller;

import com.textile.accounts.banking.dto.PaymentAccountDtos.PaymentAccountRequest;
import com.textile.accounts.banking.dto.PaymentAccountDtos.PaymentAccountResponse;
import com.textile.accounts.banking.service.PaymentAccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment-accounts")
public class PaymentAccountController {

    private final PaymentAccountService paymentAccountService;

    public PaymentAccountController(PaymentAccountService paymentAccountService) {
        this.paymentAccountService = paymentAccountService;
    }

    @GetMapping
    public ResponseEntity<Page<PaymentAccountResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String kind,
            Pageable pageable
    ) {
        return ResponseEntity.ok(paymentAccountService.search(q, active, kind, pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<PaymentAccountResponse>> getActive() {
        return ResponseEntity.ok(paymentAccountService.getActiveAccounts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentAccountResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentAccountService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PaymentAccountResponse> create(
            @Valid @RequestBody PaymentAccountRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentAccountService.create(request, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentAccountResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PaymentAccountRequest request,
            @RequestParam(required = false) Long version,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(paymentAccountService.update(id, request, version, actor));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<PaymentAccountResponse> setActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(paymentAccountService.setActive(id, active, actor));
    }
}

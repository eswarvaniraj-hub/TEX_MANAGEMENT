package com.textile.accounts.payments.controller;

import com.textile.accounts.payments.dto.PaymentDtos.PaymentRequest;
import com.textile.accounts.payments.dto.PaymentDtos.PaymentResponse;
import com.textile.accounts.payments.dto.PaymentDtos.ReversePaymentRequest;
import com.textile.accounts.payments.service.PaymentService;
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
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> search(
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable
    ) {
        return ResponseEntity.ok(paymentService.search(direction, partyId, from, to, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(request, actor));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<PaymentResponse> reverse(
            @PathVariable Long id,
            @RequestBody(required = false) ReversePaymentRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(paymentService.reverse(id, request, actor));
    }
}

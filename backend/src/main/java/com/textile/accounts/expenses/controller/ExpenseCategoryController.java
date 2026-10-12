package com.textile.accounts.expenses.controller;

import com.textile.accounts.expenses.dto.ExpenseCategoryDtos.ExpenseCategoryRequest;
import com.textile.accounts.expenses.dto.ExpenseCategoryDtos.ExpenseCategoryResponse;
import com.textile.accounts.expenses.service.ExpenseCategoryService;
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
@RequestMapping("/api/v1/expense-categories")
public class ExpenseCategoryController {

    private final ExpenseCategoryService expenseCategoryService;

    public ExpenseCategoryController(ExpenseCategoryService expenseCategoryService) {
        this.expenseCategoryService = expenseCategoryService;
    }

    @GetMapping
    public ResponseEntity<Page<ExpenseCategoryResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            Pageable pageable
    ) {
        return ResponseEntity.ok(expenseCategoryService.search(q, active, pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<ExpenseCategoryResponse>> getActive() {
        return ResponseEntity.ok(expenseCategoryService.getActiveCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseCategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseCategoryService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ExpenseCategoryResponse> create(
            @Valid @RequestBody ExpenseCategoryRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseCategoryService.create(request, actor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseCategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseCategoryRequest request,
            @RequestParam(required = false) Long version,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        return ResponseEntity.ok(expenseCategoryService.update(id, request, version, actor));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<ExpenseCategoryResponse> setActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserDetails user
    ) {
        String actor = (user != null) ? user.getUsername() : "system";
        boolean active = body.getOrDefault("active", true);
        return ResponseEntity.ok(expenseCategoryService.setActive(id, active, actor));
    }
}

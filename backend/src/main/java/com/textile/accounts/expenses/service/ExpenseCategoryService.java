package com.textile.accounts.expenses.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.expenses.dto.ExpenseCategoryDtos.ExpenseCategoryRequest;
import com.textile.accounts.expenses.dto.ExpenseCategoryDtos.ExpenseCategoryResponse;
import com.textile.accounts.expenses.model.ExpenseCategory;
import com.textile.accounts.expenses.repository.ExpenseCategoryRepository;
import com.textile.accounts.ledger.model.Account;
import com.textile.accounts.ledger.repository.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;

    public ExpenseCategoryService(
            ExpenseCategoryRepository expenseCategoryRepository,
            AccountRepository accountRepository,
            AuditService auditService
    ) {
        this.expenseCategoryRepository = expenseCategoryRepository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ExpenseCategoryResponse> search(String q, Boolean active, Pageable pageable) {
        return expenseCategoryRepository.searchCategories(q, active, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategoryResponse> getActiveCategories() {
        return expenseCategoryRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseCategoryResponse getById(Long id) {
        return expenseCategoryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Expense category not found: " + id));
    }

    @Transactional
    public ExpenseCategoryResponse create(ExpenseCategoryRequest request, String actor) {
        String name = request.name().trim();
        if (expenseCategoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Expense category already exists: " + name);
        }

        Account account;
        if (request.accountId() != null) {
            account = accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new IllegalArgumentException("Ledger Account not found: " + request.accountId()));
        } else {
            // Auto create an EXPENSE ledger account
            account = new Account();
            int num = 1;
            while (accountRepository.existsByCodeIgnoreCase("5" + String.format("%03d", num))) {
                num++;
            }
            account.setCode("5" + String.format("%03d", num));
            account.setName(name);
            account.setAccountType("EXPENSE");
            account.setReportGroup("INDIRECT_EXPENSES");
            account.setPostable(true);
            account.setSystem(false);
            account.setActive(true);
            account = accountRepository.save(account);
        }

        ExpenseCategory ec = new ExpenseCategory();
        ec.setName(name);
        ec.setAccount(account);
        ec.setActive(request.active() != null ? request.active() : true);
        ec.setCreatedAt(Instant.now());
        ec.setUpdatedAt(Instant.now());

        ExpenseCategory saved = expenseCategoryRepository.save(ec);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "CREATE",
                "EXPENSE_CATEGORY",
                saved.getId().toString(),
                "Created Expense Category: " + saved.getName(),
                Map.of("name", saved.getName())
        );

        return toResponse(saved);
    }

    @Transactional
    public ExpenseCategoryResponse update(Long id, ExpenseCategoryRequest request, Long submittedVersion, String actor) {
        ExpenseCategory ec = expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense category not found: " + id));

        if (submittedVersion != null && !Objects.equals(ec.getVersion(), submittedVersion)) {
            throw new IllegalStateException("OPTIMISTIC_LOCK_CONFLICT: Expense category modified by another transaction.");
        }

        ec.setName(request.name().trim());
        if (request.accountId() != null && !request.accountId().equals(ec.getAccount().getId())) {
            Account account = accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new IllegalArgumentException("Ledger Account not found: " + request.accountId()));
            ec.setAccount(account);
        }
        if (request.active() != null) {
            ec.setActive(request.active());
        }
        ec.setUpdatedAt(Instant.now());

        ExpenseCategory updated = expenseCategoryRepository.save(ec);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "UPDATE",
                "EXPENSE_CATEGORY",
                updated.getId().toString(),
                "Updated Expense Category: " + updated.getName(),
                Map.of("name", updated.getName())
        );

        return toResponse(updated);
    }

    @Transactional
    public ExpenseCategoryResponse setActive(Long id, boolean active, String actor) {
        ExpenseCategory ec = expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense category not found: " + id));
        ec.setActive(active);
        ec.setUpdatedAt(Instant.now());
        ExpenseCategory updated = expenseCategoryRepository.save(ec);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                active ? "ACTIVATE" : "DEACTIVATE",
                "EXPENSE_CATEGORY",
                updated.getId().toString(),
                (active ? "Activated" : "Deactivated") + " Expense Category: " + updated.getName(),
                Map.of("active", active)
        );

        return toResponse(updated);
    }

    public ExpenseCategoryResponse toResponse(ExpenseCategory ec) {
        return new ExpenseCategoryResponse(
                ec.getId(),
                ec.getName(),
                ec.getAccount() != null ? ec.getAccount().getId() : null,
                ec.getAccount() != null ? ec.getAccount().getCode() : null,
                ec.getAccount() != null ? ec.getAccount().getName() : null,
                ec.isActive(),
                ec.getVersion()
        );
    }
}

package com.textile.accounts.ledger.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.ledger.dto.AccountDtos.AccountRequest;
import com.textile.accounts.ledger.dto.AccountDtos.AccountResponse;
import com.textile.accounts.ledger.model.Account;
import com.textile.accounts.ledger.repository.AccountRepository;
import com.textile.accounts.ledger.repository.JournalLineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;
    private final AuditService auditService;

    public AccountService(
            AccountRepository accountRepository,
            JournalLineRepository journalLineRepository,
            AuditService auditService
    ) {
        this.accountRepository = accountRepository;
        this.journalLineRepository = journalLineRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<AccountResponse> searchAccounts(String q, Boolean active, String accountType, Pageable pageable) {
        return accountRepository.searchAccounts(q, active, accountType, pageable)
                .map(this::toResponseWithBalance);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAllByOrderByCodeAsc().stream()
                .map(this::toResponseWithBalance)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getById(Long id) {
        return accountRepository.findById(id)
                .map(this::toResponseWithBalance)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));
    }

    @Transactional
    public AccountResponse createAccount(AccountRequest request, String actor) {
        String cleanCode = request.code().trim().toUpperCase();
        if (accountRepository.existsByCodeIgnoreCase(cleanCode)) {
            throw new IllegalArgumentException("Account code already exists: " + cleanCode);
        }

        Account account = new Account();
        account.setCode(cleanCode);
        account.setName(request.name().trim());
        account.setAccountType(request.accountType().toUpperCase());
        account.setSystemRole(request.systemRole());
        account.setReportGroup(request.reportGroup());
        account.setPostable(request.isPostable() != null ? request.isPostable() : true);
        account.setSystem(false);
        account.setActive(request.active() != null ? request.active() : true);

        if (request.parentId() != null) {
            Account parent = accountRepository.findById(request.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent account not found: " + request.parentId()));
            account.setParent(parent);
        }

        account.setCreatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());

        Account saved = accountRepository.save(account);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "CREATE",
                "ACCOUNT",
                saved.getId().toString(),
                "Created Account: " + saved.getName() + " (" + saved.getCode() + ")",
                Map.of("code", saved.getCode(), "type", saved.getAccountType())
        );

        return toResponseWithBalance(saved);
    }

    @Transactional
    public AccountResponse updateAccount(Long id, AccountRequest request, Long submittedVersion, String actor) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with ID: " + id));

        if (submittedVersion != null && !Objects.equals(account.getVersion(), submittedVersion)) {
            throw new IllegalStateException("OPTIMISTIC_LOCK_CONFLICT: Account was modified by another transaction.");
        }

        if (account.isSystem()) {
            // System accounts cannot change code or system role
            account.setName(request.name().trim());
            account.setReportGroup(request.reportGroup());
        } else {
            account.setName(request.name().trim());
            account.setReportGroup(request.reportGroup());
            if (request.isPostable() != null) {
                account.setPostable(request.isPostable());
            }
            if (request.active() != null) {
                account.setActive(request.active());
            }
            if (request.parentId() != null) {
                Account parent = accountRepository.findById(request.parentId())
                        .orElseThrow(() -> new IllegalArgumentException("Parent account not found: " + request.parentId()));
                account.setParent(parent);
            } else {
                account.setParent(null);
            }
        }
        account.setUpdatedAt(Instant.now());

        Account updated = accountRepository.save(account);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "UPDATE",
                "ACCOUNT",
                updated.getId().toString(),
                "Updated Account: " + updated.getName() + " (" + updated.getCode() + ")",
                Map.of("code", updated.getCode())
        );

        return toResponseWithBalance(updated);
    }

    private AccountResponse toResponseWithBalance(Account a) {
        BigDecimal balance = BigDecimal.ZERO;
        if (a.isPostable()) {
            // ASSET and EXPENSE normally have debit balances; LIABILITY, EQUITY, INCOME have credit balances
            if ("ASSET".equalsIgnoreCase(a.getAccountType()) || "EXPENSE".equalsIgnoreCase(a.getAccountType())) {
                balance = journalLineRepository.getNetDebitBalance(a.getId(), null);
            } else {
                balance = journalLineRepository.getNetCreditBalance(a.getId(), null);
            }
        }
        return new AccountResponse(
                a.getId(),
                a.getCode(),
                a.getName(),
                a.getAccountType(),
                a.getSystemRole(),
                a.getReportGroup(),
                a.getParent() != null ? a.getParent().getId() : null,
                a.getParent() != null ? a.getParent().getName() : null,
                a.isPostable(),
                a.isSystem(),
                a.isActive(),
                balance != null ? balance : BigDecimal.ZERO,
                a.getVersion()
        );
    }
}

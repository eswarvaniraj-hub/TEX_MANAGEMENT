package com.textile.accounts.banking.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.banking.dto.PaymentAccountDtos.PaymentAccountRequest;
import com.textile.accounts.banking.dto.PaymentAccountDtos.PaymentAccountResponse;
import com.textile.accounts.banking.model.PaymentAccount;
import com.textile.accounts.banking.repository.PaymentAccountRepository;
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
public class PaymentAccountService {

    private final PaymentAccountRepository paymentAccountRepository;
    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;
    private final AuditService auditService;

    public PaymentAccountService(
            PaymentAccountRepository paymentAccountRepository,
            AccountRepository accountRepository,
            JournalLineRepository journalLineRepository,
            AuditService auditService
    ) {
        this.paymentAccountRepository = paymentAccountRepository;
        this.accountRepository = accountRepository;
        this.journalLineRepository = journalLineRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<PaymentAccountResponse> search(String q, Boolean active, String kind, Pageable pageable) {
        return paymentAccountRepository.searchPaymentAccounts(q, active, kind, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<PaymentAccountResponse> getActiveAccounts() {
        return paymentAccountRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentAccountResponse getById(Long id) {
        return paymentAccountRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Payment account not found with ID: " + id));
    }

    @Transactional
    public PaymentAccountResponse create(PaymentAccountRequest request, String actor) {
        String name = request.name().trim();
        if (paymentAccountRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Payment account with name already exists: " + name);
        }

        Account account;
        if (request.accountId() != null) {
            account = accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new IllegalArgumentException("Ledger Account not found: " + request.accountId()));
        } else {
            // Auto create an ASSET ledger account
            account = new Account();
            String kind = request.kind().toUpperCase();
            String prefix = "CASH".equals(kind) ? "101" : "102";
            int num = 1;
            while (accountRepository.existsByCodeIgnoreCase(prefix + String.format("%02d", num))) {
                num++;
            }
            account.setCode(prefix + String.format("%02d", num));
            account.setName(name);
            account.setAccountType("ASSET");
            account.setReportGroup("CURRENT_ASSETS");
            account.setPostable(true);
            account.setSystem(false);
            account.setActive(true);
            account = accountRepository.save(account);
        }

        PaymentAccount pa = new PaymentAccount();
        pa.setName(name);
        pa.setKind(request.kind().toUpperCase());
        pa.setAccount(account);
        pa.setBankName(request.bankName());
        pa.setAccountLast4(request.accountLast4());
        pa.setActive(request.active() != null ? request.active() : true);
        pa.setCreatedAt(Instant.now());
        pa.setUpdatedAt(Instant.now());

        PaymentAccount saved = paymentAccountRepository.save(pa);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "CREATE",
                "PAYMENT_ACCOUNT",
                saved.getId().toString(),
                "Created Payment Account: " + saved.getName() + " (" + saved.getKind() + ")",
                Map.of("name", saved.getName(), "kind", saved.getKind())
        );

        return toResponse(saved);
    }

    @Transactional
    public PaymentAccountResponse update(Long id, PaymentAccountRequest request, Long submittedVersion, String actor) {
        PaymentAccount pa = paymentAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment account not found: " + id));

        if (submittedVersion != null && !Objects.equals(pa.getVersion(), submittedVersion)) {
            throw new IllegalStateException("OPTIMISTIC_LOCK_CONFLICT: Payment account modified by another transaction.");
        }

        pa.setName(request.name().trim());
        pa.setBankName(request.bankName());
        pa.setAccountLast4(request.accountLast4());
        if (request.active() != null) {
            pa.setActive(request.active());
        }
        pa.setUpdatedAt(Instant.now());

        PaymentAccount updated = paymentAccountRepository.save(pa);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "UPDATE",
                "PAYMENT_ACCOUNT",
                updated.getId().toString(),
                "Updated Payment Account: " + updated.getName(),
                Map.of("name", updated.getName())
        );

        return toResponse(updated);
    }

    @Transactional
    public PaymentAccountResponse setActive(Long id, boolean active, String actor) {
        PaymentAccount pa = paymentAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment account not found: " + id));
        pa.setActive(active);
        pa.setUpdatedAt(Instant.now());
        PaymentAccount updated = paymentAccountRepository.save(pa);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                active ? "ACTIVATE" : "DEACTIVATE",
                "PAYMENT_ACCOUNT",
                updated.getId().toString(),
                (active ? "Activated" : "Deactivated") + " Payment Account: " + updated.getName(),
                Map.of("active", active)
        );

        return toResponse(updated);
    }

    public PaymentAccountResponse toResponse(PaymentAccount pa) {
        BigDecimal balance = BigDecimal.ZERO;
        if (pa.getAccount() != null) {
            balance = journalLineRepository.getNetDebitBalance(pa.getAccount().getId(), null);
        }
        return new PaymentAccountResponse(
                pa.getId(),
                pa.getName(),
                pa.getKind(),
                pa.getAccount() != null ? pa.getAccount().getId() : null,
                pa.getAccount() != null ? pa.getAccount().getCode() : null,
                pa.getAccount() != null ? pa.getAccount().getName() : null,
                pa.getBankName(),
                pa.getAccountLast4(),
                pa.isActive(),
                balance != null ? balance : BigDecimal.ZERO,
                pa.getVersion()
        );
    }
}

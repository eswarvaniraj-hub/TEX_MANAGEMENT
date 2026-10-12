package com.textile.accounts.expenses.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.banking.model.PaymentAccount;
import com.textile.accounts.banking.repository.PaymentAccountRepository;
import com.textile.accounts.expenses.dto.ExpenseDtos.ExpenseRequest;
import com.textile.accounts.expenses.dto.ExpenseDtos.ExpenseResponse;
import com.textile.accounts.expenses.dto.ExpenseDtos.ReverseExpenseRequest;
import com.textile.accounts.expenses.model.Expense;
import com.textile.accounts.expenses.model.ExpenseCategory;
import com.textile.accounts.expenses.repository.ExpenseCategoryRepository;
import com.textile.accounts.expenses.repository.ExpenseRepository;
import com.textile.accounts.ledger.model.JournalEntry;
import com.textile.accounts.ledger.service.PostingRequest;
import com.textile.accounts.ledger.service.PostingRequest.PostingLineRequest;
import com.textile.accounts.ledger.service.PostingService;
import com.textile.accounts.settings.model.FinancialYear;
import com.textile.accounts.settings.repository.FinancialYearRepository;
import com.textile.accounts.settings.service.DocumentNumberService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final FinancialYearRepository financialYearRepository;
    private final PostingService postingService;
    private final DocumentNumberService documentNumberService;
    private final AuditService auditService;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            ExpenseCategoryRepository categoryRepository,
            PaymentAccountRepository paymentAccountRepository,
            FinancialYearRepository financialYearRepository,
            PostingService postingService,
            DocumentNumberService documentNumberService,
            AuditService auditService
    ) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.paymentAccountRepository = paymentAccountRepository;
        this.financialYearRepository = financialYearRepository;
        this.postingService = postingService;
        this.documentNumberService = documentNumberService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> search(LocalDate from, LocalDate to, Long categoryId, String status, Pageable pageable) {
        return expenseRepository.searchExpenses(from, to, categoryId, status, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getById(Long id) {
        return expenseRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + id));
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request, String actor) {
        postingService.validateLockDate(request.expenseDate());

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        FinancialYear fy = financialYearRepository.findByDate(request.expenseDate())
                .orElseThrow(() -> new IllegalArgumentException("No Financial Year for date: " + request.expenseDate()));

        ExpenseCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Expense Category not found: " + request.categoryId()));

        PaymentAccount paymentAccount = paymentAccountRepository.findById(request.paymentAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Payment Account not found: " + request.paymentAccountId()));

        String expenseNo = documentNumberService.getNextDocumentNumber("EXP", request.expenseDate());

        List<PostingLineRequest> lines = new ArrayList<>();
        // Debit Expense Account
        lines.add(PostingLineRequest.debit(
                category.getAccount(),
                request.amount(),
                null,
                null,
                request.description() + (request.referenceNo() != null ? " (Ref: " + request.referenceNo() + ")" : "")
        ));
        // Credit Payment Account
        lines.add(PostingLineRequest.credit(
                paymentAccount.getAccount(),
                request.amount(),
                null,
                null,
                "Expense paid via " + paymentAccount.getName()
        ));

        PostingRequest postingRequest = new PostingRequest(
                request.expenseDate(),
                "EXPENSE",
                null,
                "Expense " + expenseNo + ": " + category.getName() + " - " + request.description(),
                actor != null ? actor : "system",
                lines
        );

        JournalEntry journalEntry = postingService.post(postingRequest);

        Expense expense = new Expense();
        expense.setExpenseNo(expenseNo);
        expense.setFinancialYear(fy);
        expense.setExpenseDate(request.expenseDate());
        expense.setCategory(category);
        expense.setDescription(request.description().trim());
        expense.setAmount(request.amount());
        expense.setPaymentAccount(paymentAccount);
        expense.setReferenceNo(request.referenceNo());
        expense.setNotes(request.notes());
        expense.setStatus("POSTED");
        expense.setJournalEntry(journalEntry);
        expense.setCreatedAt(Instant.now());

        Expense saved = expenseRepository.save(expense);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "CREATE_EXPENSE",
                "EXPENSE",
                saved.getId().toString(),
                "Recorded Expense: " + expenseNo + " (" + saved.getAmount() + ")",
                Map.of("expenseNo", expenseNo, "amount", saved.getAmount(), "category", category.getName())
        );

        return toResponse(saved);
    }

    @Transactional
    public ExpenseResponse reverse(Long id, ReverseExpenseRequest request, String actor) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found: " + id));

        if (!"POSTED".equals(expense.getStatus())) {
            throw new IllegalStateException("Only POSTED expenses can be reversed.");
        }

        LocalDate reversalDate = LocalDate.now();
        postingService.validateLockDate(reversalDate);

        JournalEntry reversalEntry = postingService.reverseEntry(
                expense.getJournalEntry().getId(),
                reversalDate,
                request != null ? request.reason() : "Expense reversal",
                actor
        );

        expense.setStatus("REVERSED");
        expense.setReversalEntry(reversalEntry);
        expense.setReversedAt(Instant.now());
        expense.setReversalReason(request != null ? request.reason() : "Cancelled");

        Expense saved = expenseRepository.save(expense);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "REVERSE_EXPENSE",
                "EXPENSE",
                saved.getId().toString(),
                "Reversed Expense: " + saved.getExpenseNo(),
                Map.of("expenseNo", saved.getExpenseNo(), "reason", saved.getReversalReason())
        );

        return toResponse(saved);
    }

    public ExpenseResponse toResponse(Expense e) {
        return new ExpenseResponse(
                e.getId(),
                e.getExpenseNo(),
                e.getExpenseDate(),
                e.getCategory().getId(),
                e.getCategory().getName(),
                e.getDescription(),
                e.getAmount(),
                e.getPaymentAccount().getId(),
                e.getPaymentAccount().getName(),
                e.getReferenceNo(),
                e.getNotes(),
                e.getStatus(),
                e.getJournalEntry() != null ? e.getJournalEntry().getId() : null,
                e.getReversalEntry() != null ? e.getReversalEntry().getId() : null,
                e.getReversedAt(),
                e.getReversalReason()
        );
    }
}

package com.textile.accounts.banking.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.banking.dto.BankingDtos.AccountStatementLine;
import com.textile.accounts.banking.dto.BankingDtos.FundTransferRequest;
import com.textile.accounts.banking.dto.BankingDtos.FundTransferResponse;
import com.textile.accounts.banking.dto.BankingDtos.ReverseTransferRequest;
import com.textile.accounts.banking.model.FundTransfer;
import com.textile.accounts.banking.model.PaymentAccount;
import com.textile.accounts.banking.repository.FundTransferRepository;
import com.textile.accounts.banking.repository.PaymentAccountRepository;
import com.textile.accounts.ledger.model.JournalEntry;
import com.textile.accounts.ledger.model.JournalLine;
import com.textile.accounts.ledger.repository.JournalLineRepository;
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
import java.util.Objects;

@Service
public class BankingService {

    private final FundTransferRepository fundTransferRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final JournalLineRepository journalLineRepository;
    private final FinancialYearRepository financialYearRepository;
    private final PostingService postingService;
    private final DocumentNumberService documentNumberService;
    private final AuditService auditService;

    public BankingService(
            FundTransferRepository fundTransferRepository,
            PaymentAccountRepository paymentAccountRepository,
            JournalLineRepository journalLineRepository,
            FinancialYearRepository financialYearRepository,
            PostingService postingService,
            DocumentNumberService documentNumberService,
            AuditService auditService
    ) {
        this.fundTransferRepository = fundTransferRepository;
        this.paymentAccountRepository = paymentAccountRepository;
        this.journalLineRepository = journalLineRepository;
        this.financialYearRepository = financialYearRepository;
        this.postingService = postingService;
        this.documentNumberService = documentNumberService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<FundTransferResponse> searchTransfers(LocalDate from, LocalDate to, Pageable pageable) {
        return fundTransferRepository.searchTransfers(from, to, pageable)
                .map(this::toTransferResponse);
    }

    @Transactional
    public FundTransferResponse transferFunds(FundTransferRequest request, String actor) {
        postingService.validateLockDate(request.transferDate());

        if (Objects.equals(request.fromPaymentAccountId(), request.toPaymentAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same.");
        }

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero.");
        }

        FinancialYear fy = financialYearRepository.findByDate(request.transferDate())
                .orElseThrow(() -> new IllegalArgumentException("No Financial Year for date: " + request.transferDate()));

        PaymentAccount fromAccount = paymentAccountRepository.findById(request.fromPaymentAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + request.fromPaymentAccountId()));

        PaymentAccount toAccount = paymentAccountRepository.findById(request.toPaymentAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + request.toPaymentAccountId()));

        String transferNo = documentNumberService.getNextDocumentNumber("FT", request.transferDate());

        List<PostingLineRequest> lines = new ArrayList<>();
        // Debit destination account
        lines.add(PostingLineRequest.debit(
                toAccount.getAccount(),
                request.amount(),
                null,
                null,
                "Transfer in from " + fromAccount.getName() + (request.reference() != null ? " Ref: " + request.reference() : "")
        ));
        // Credit source account
        lines.add(PostingLineRequest.credit(
                fromAccount.getAccount(),
                request.amount(),
                null,
                null,
                "Transfer out to " + toAccount.getName() + (request.reference() != null ? " Ref: " + request.reference() : "")
        ));

        PostingRequest postingRequest = new PostingRequest(
                request.transferDate(),
                "FUND_TRANSFER",
                null,
                "Transfer " + transferNo + ": " + fromAccount.getName() + " -> " + toAccount.getName(),
                actor != null ? actor : "system",
                lines
        );

        JournalEntry journalEntry = postingService.post(postingRequest);

        FundTransfer ft = new FundTransfer();
        ft.setTransferNo(transferNo);
        ft.setFinancialYear(fy);
        ft.setTransferDate(request.transferDate());
        ft.setFromPaymentAccount(fromAccount);
        ft.setToPaymentAccount(toAccount);
        ft.setAmount(request.amount());
        ft.setReference(request.reference());
        ft.setNotes(request.notes());
        ft.setStatus("POSTED");
        ft.setJournalEntry(journalEntry);
        ft.setCreatedAt(Instant.now());
        ft.setCreatedBy(actor != null ? actor : "system");

        FundTransfer saved = fundTransferRepository.save(ft);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "FUND_TRANSFER",
                "FUND_TRANSFER",
                saved.getId().toString(),
                "Transferred " + saved.getAmount() + " from " + fromAccount.getName() + " to " + toAccount.getName(),
                Map.of("transferNo", transferNo, "amount", saved.getAmount())
        );

        return toTransferResponse(saved);
    }

    @Transactional
    public FundTransferResponse reverseTransfer(Long id, ReverseTransferRequest request, String actor) {
        FundTransfer ft = fundTransferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fund transfer not found: " + id));

        if (!"POSTED".equals(ft.getStatus())) {
            throw new IllegalStateException("Only POSTED transfers can be reversed.");
        }

        LocalDate reversalDate = LocalDate.now();
        postingService.validateLockDate(reversalDate);

        JournalEntry reversalEntry = postingService.reverseEntry(
                ft.getJournalEntry().getId(),
                reversalDate,
                request != null ? request.reason() : "Fund transfer reversal",
                actor
        );

        ft.setStatus("REVERSED");
        ft.setReversalEntry(reversalEntry);
        ft.setReversedAt(Instant.now());
        ft.setReversalReason(request != null ? request.reason() : "Cancelled");

        FundTransfer saved = fundTransferRepository.save(ft);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "REVERSE_FUND_TRANSFER",
                "FUND_TRANSFER",
                saved.getId().toString(),
                "Reversed Fund Transfer: " + saved.getTransferNo(),
                Map.of("transferNo", saved.getTransferNo())
        );

        return toTransferResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountStatementLine> getAccountStatement(Long paymentAccountId, LocalDate from, LocalDate to) {
        PaymentAccount pa = paymentAccountRepository.findById(paymentAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Payment account not found: " + paymentAccountId));

        Long accountId = pa.getAccount().getId();
        BigDecimal runningBalance = BigDecimal.ZERO;

        if (from != null) {
            BigDecimal opening = journalLineRepository.getNetDebitBalance(accountId, from.minusDays(1));
            if (opening != null) {
                runningBalance = opening;
            }
        }

        List<JournalLine> lines = journalLineRepository.findAccountLedgerLines(accountId, from, to);
        List<AccountStatementLine> result = new ArrayList<>();

        if (from != null && runningBalance.compareTo(BigDecimal.ZERO) != 0) {
            result.add(new AccountStatementLine(
                    from,
                    "-",
                    "Opening Balance",
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    runningBalance
            ));
        }

        for (JournalLine line : lines) {
            BigDecimal debit = line.getDebit();
            BigDecimal credit = line.getCredit();
            runningBalance = runningBalance.add(debit).subtract(credit);

            result.add(new AccountStatementLine(
                    line.getJournalEntry().getEntryDate(),
                    line.getJournalEntry().getEntryNo(),
                    line.getNarration() != null ? line.getNarration() : line.getJournalEntry().getDescription(),
                    debit,
                    credit,
                    runningBalance
            ));
        }

        return result;
    }

    public FundTransferResponse toTransferResponse(FundTransfer ft) {
        return new FundTransferResponse(
                ft.getId(),
                ft.getTransferNo(),
                ft.getTransferDate(),
                ft.getFromPaymentAccount().getId(),
                ft.getFromPaymentAccount().getName(),
                ft.getToPaymentAccount().getId(),
                ft.getToPaymentAccount().getName(),
                ft.getAmount(),
                ft.getReference(),
                ft.getNotes(),
                ft.getStatus(),
                ft.getJournalEntry() != null ? ft.getJournalEntry().getId() : null,
                ft.getReversalEntry() != null ? ft.getReversalEntry().getId() : null,
                ft.getReversedAt(),
                ft.getReversalReason()
        );
    }
}

package com.textile.accounts.ledger.service;

import com.textile.accounts.ledger.model.JournalEntry;
import com.textile.accounts.ledger.model.JournalLine;
import com.textile.accounts.ledger.repository.JournalEntryRepository;
import com.textile.accounts.settings.model.BusinessProfile;
import com.textile.accounts.settings.model.FinancialYear;
import com.textile.accounts.settings.repository.BusinessProfileRepository;
import com.textile.accounts.settings.repository.FinancialYearRepository;
import com.textile.accounts.settings.service.DocumentNumberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PostingService {

    private final JournalEntryRepository journalEntryRepository;
    private final FinancialYearRepository financialYearRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final DocumentNumberService documentNumberService;

    public PostingService(
            JournalEntryRepository journalEntryRepository,
            FinancialYearRepository financialYearRepository,
            BusinessProfileRepository businessProfileRepository,
            DocumentNumberService documentNumberService
    ) {
        this.journalEntryRepository = journalEntryRepository;
        this.financialYearRepository = financialYearRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.documentNumberService = documentNumberService;
    }

    /**
     * Atomically posts a balanced double-entry journal transaction.
     * Validates lock date, financial year, debit-credit equality, account posting status, and party tags.
     */
    @Transactional
    public JournalEntry post(PostingRequest request) {
        validateLockDate(request.entryDate());

        FinancialYear fy = financialYearRepository.findByDate(request.entryDate())
                .orElseThrow(() -> new IllegalArgumentException("No financial year configured for date: " + request.entryDate()));

        List<PostingRequest.PostingLineRequest> lines = request.lines();
        if (lines == null || lines.size() < 2) {
            throw new IllegalArgumentException("A journal entry must contain at least 2 lines.");
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (PostingRequest.PostingLineRequest line : lines) {
            if (line.account() == null) {
                throw new IllegalArgumentException("Account is required on every journal line.");
            }
            if (!line.account().isActive()) {
                throw new IllegalArgumentException("Cannot post to inactive account: " + line.account().getCode());
            }
            if (!line.account().isPostable()) {
                throw new IllegalArgumentException("Cannot post to non-postable header account: " + line.account().getCode());
            }

            BigDecimal debit = line.debit() != null ? line.debit() : BigDecimal.ZERO;
            BigDecimal credit = line.credit() != null ? line.credit() : BigDecimal.ZERO;

            boolean hasDebit = debit.compareTo(BigDecimal.ZERO) > 0;
            boolean hasCredit = credit.compareTo(BigDecimal.ZERO) > 0;

            if (hasDebit == hasCredit) {
                throw new IllegalArgumentException("Each journal line must have exactly one of debit > 0 or credit > 0.");
            }

            // Party control account validation
            String systemRole = line.account().getSystemRole();
            if ("AR_CONTROL".equals(systemRole)) {
                if (line.customer() == null || line.supplier() != null) {
                    throw new IllegalArgumentException("Accounts Receivable line must be tagged with a customer and cannot have a supplier.");
                }
            } else if ("AP_CONTROL".equals(systemRole)) {
                if (line.supplier() == null || line.customer() != null) {
                    throw new IllegalArgumentException("Accounts Payable line must be tagged with a supplier and cannot have a customer.");
                }
            } else {
                if (line.customer() != null || line.supplier() != null) {
                    throw new IllegalArgumentException("Party tags (customer/supplier) are permitted only on AR_CONTROL and AP_CONTROL lines.");
                }
            }

            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException(String.format(
                    "Unbalanced journal entry: Total Debits (%s) != Total Credits (%s)",
                    totalDebit.toPlainString(), totalCredit.toPlainString()
            ));
        }

        String entryNo = documentNumberService.getNextDocumentNumber("JV", request.entryDate());

        JournalEntry entry = new JournalEntry();
        entry.setEntryNo(entryNo);
        entry.setEntryDate(request.entryDate());
        entry.setFinancialYear(fy);
        entry.setSourceType(request.sourceType());
        entry.setSourceId(request.sourceId());
        entry.setDescription(request.description());
        entry.setCreatedBy(request.createdBy() != null ? request.createdBy() : "system");

        int lineNo = 1;
        for (PostingRequest.PostingLineRequest lineReq : lines) {
            JournalLine line = new JournalLine();
            line.setLineNo(lineNo++);
            line.setAccount(lineReq.account());
            line.setDebit(lineReq.debit() != null ? lineReq.debit() : BigDecimal.ZERO);
            line.setCredit(lineReq.credit() != null ? lineReq.credit() : BigDecimal.ZERO);
            line.setCustomer(lineReq.customer());
            line.setSupplier(lineReq.supplier());
            line.setNarration(lineReq.narration());
            entry.addLine(line);
        }

        return journalEntryRepository.save(entry);
    }

    /**
     * Atomically reverses a posted journal entry by generating an exact mirror entry.
     */
    @Transactional
    public JournalEntry reverseEntry(Long journalEntryId, LocalDate reversalDate, String reason, String actor) {
        JournalEntry original = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found: " + journalEntryId));

        if (original.getReversesEntry() != null) {
            throw new IllegalStateException("Cannot reverse a reversal entry.");
        }
        if (journalEntryRepository.existsByReversesEntryId(original.getId())) {
            throw new IllegalStateException("Journal entry " + original.getEntryNo() + " has already been reversed.");
        }

        if (reversalDate.isBefore(original.getEntryDate())) {
            throw new IllegalArgumentException("Reversal date cannot be earlier than original entry date: " + original.getEntryDate());
        }
        validateLockDate(reversalDate);

        FinancialYear fy = financialYearRepository.findByDate(reversalDate)
                .orElseThrow(() -> new IllegalArgumentException("No financial year configured for reversal date: " + reversalDate));

        String reversalNo = documentNumberService.getNextDocumentNumber("JV", reversalDate);

        JournalEntry reversal = new JournalEntry();
        reversal.setEntryNo(reversalNo);
        reversal.setEntryDate(reversalDate);
        reversal.setFinancialYear(fy);
        reversal.setSourceType(original.getSourceType());
        reversal.setSourceId(original.getSourceId());
        reversal.setDescription("Reversal of " + original.getEntryNo() + ": " + (reason != null ? reason : "Correction"));
        reversal.setReversesEntry(original);
        reversal.setCreatedBy(actor != null ? actor : "system");

        int lineNo = 1;
        for (JournalLine origLine : original.getLines()) {
            JournalLine revLine = new JournalLine();
            revLine.setLineNo(lineNo++);
            revLine.setAccount(origLine.getAccount());
            // Mirror: debit becomes credit, credit becomes debit
            revLine.setDebit(origLine.getCredit());
            revLine.setCredit(origLine.getDebit());
            revLine.setCustomer(origLine.getCustomer());
            revLine.setSupplier(origLine.getSupplier());
            revLine.setNarration("Reversal: " + (origLine.getNarration() != null ? origLine.getNarration() : ""));
            reversal.addLine(revLine);
        }

        return journalEntryRepository.save(reversal);
    }

    public void validateLockDate(LocalDate date) {
        businessProfileRepository.getProfile().ifPresent(profile -> {
            LocalDate lockDate = profile.getLockDate();
            if (lockDate != null && (date.isBefore(lockDate) || date.isEqual(lockDate))) {
                throw new IllegalStateException("Cannot record transaction: date " + date + " is on or before period lock date " + lockDate);
            }
        });
    }
}

package com.textile.accounts.ledger.service;

import com.textile.accounts.ledger.dto.LedgerDtos.JournalEntryResponse;
import com.textile.accounts.ledger.dto.LedgerDtos.JournalLineDto;
import com.textile.accounts.ledger.dto.LedgerDtos.PartyLedgerLine;
import com.textile.accounts.ledger.model.JournalEntry;
import com.textile.accounts.ledger.model.JournalLine;
import com.textile.accounts.ledger.repository.JournalEntryRepository;
import com.textile.accounts.ledger.repository.JournalLineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class LedgerService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final IntegrityCheckService integrityCheckService;

    public LedgerService(
            JournalEntryRepository journalEntryRepository,
            JournalLineRepository journalLineRepository,
            IntegrityCheckService integrityCheckService
    ) {
        this.journalEntryRepository = journalEntryRepository;
        this.journalLineRepository = journalLineRepository;
        this.integrityCheckService = integrityCheckService;
    }

    @Transactional(readOnly = true)
    public Page<JournalEntryResponse> searchEntries(LocalDate from, LocalDate to, String sourceType, Pageable pageable) {
        return journalEntryRepository.searchEntries(from, to, sourceType, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public JournalEntryResponse getById(Long id) {
        return journalEntryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Journal Entry not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<PartyLedgerLine> getCustomerLedger(Long customerId, LocalDate from, LocalDate to) {
        BigDecimal runningBalance = BigDecimal.ZERO;
        if (from != null) {
            BigDecimal opening = journalLineRepository.getCustomerBalance(customerId, from.minusDays(1));
            if (opening != null) {
                runningBalance = opening;
            }
        }

        List<JournalLine> lines = journalLineRepository.findCustomerLedgerLines(customerId, from, to);
        List<PartyLedgerLine> result = new ArrayList<>();

        if (from != null && runningBalance.compareTo(BigDecimal.ZERO) != 0) {
            result.add(new PartyLedgerLine(from, "-", "Opening Balance", BigDecimal.ZERO, BigDecimal.ZERO, runningBalance));
        }

        for (JournalLine line : lines) {
            BigDecimal dr = line.getDebit();
            BigDecimal cr = line.getCredit();
            runningBalance = runningBalance.add(dr).subtract(cr);

            result.add(new PartyLedgerLine(
                    line.getJournalEntry().getEntryDate(),
                    line.getJournalEntry().getEntryNo(),
                    line.getNarration() != null ? line.getNarration() : line.getJournalEntry().getDescription(),
                    dr,
                    cr,
                    runningBalance
            ));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<PartyLedgerLine> getSupplierLedger(Long supplierId, LocalDate from, LocalDate to) {
        BigDecimal runningBalance = BigDecimal.ZERO;
        if (from != null) {
            BigDecimal opening = journalLineRepository.getSupplierBalance(supplierId, from.minusDays(1));
            if (opening != null) {
                runningBalance = opening;
            }
        }

        List<JournalLine> lines = journalLineRepository.findSupplierLedgerLines(supplierId, from, to);
        List<PartyLedgerLine> result = new ArrayList<>();

        if (from != null && runningBalance.compareTo(BigDecimal.ZERO) != 0) {
            result.add(new PartyLedgerLine(from, "-", "Opening Balance", BigDecimal.ZERO, BigDecimal.ZERO, runningBalance));
        }

        for (JournalLine line : lines) {
            BigDecimal dr = line.getDebit();
            BigDecimal cr = line.getCredit();
            runningBalance = runningBalance.add(cr).subtract(dr); // Credit balance for suppliers

            result.add(new PartyLedgerLine(
                    line.getJournalEntry().getEntryDate(),
                    line.getJournalEntry().getEntryNo(),
                    line.getNarration() != null ? line.getNarration() : line.getJournalEntry().getDescription(),
                    dr,
                    cr,
                    runningBalance
            ));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public IntegrityCheckResult checkIntegrity() {
        return integrityCheckService.runIntegrityCheck();
    }

    public JournalEntryResponse toResponse(JournalEntry entry) {
        List<JournalLineDto> lineDtos = entry.getLines().stream().map(l -> new JournalLineDto(
                l.getLineNo(),
                l.getAccount().getId(),
                l.getAccount().getCode(),
                l.getAccount().getName(),
                l.getDebit(),
                l.getCredit(),
                l.getCustomer() != null ? l.getCustomer().getId() : null,
                l.getCustomer() != null ? l.getCustomer().getName() : null,
                l.getSupplier() != null ? l.getSupplier().getId() : null,
                l.getSupplier() != null ? l.getSupplier().getName() : null,
                l.getNarration()
        )).toList();

        return new JournalEntryResponse(
                entry.getId(),
                entry.getEntryNo(),
                entry.getEntryDate(),
                entry.getSourceType(),
                entry.getSourceId(),
                entry.getDescription(),
                entry.getCreatedBy(),
                entry.getReversesEntry() != null ? entry.getReversesEntry().getId() : null,
                entry.getReversesEntry() != null ? entry.getReversesEntry().getEntryNo() : null,
                lineDtos
        );
    }
}

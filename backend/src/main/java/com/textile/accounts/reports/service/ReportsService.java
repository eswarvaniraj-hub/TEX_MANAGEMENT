package com.textile.accounts.reports.service;

import com.textile.accounts.ledger.model.Account;
import com.textile.accounts.ledger.model.JournalEntry;
import com.textile.accounts.ledger.model.JournalLine;
import com.textile.accounts.ledger.repository.AccountRepository;
import com.textile.accounts.ledger.repository.JournalEntryRepository;
import com.textile.accounts.ledger.repository.JournalLineRepository;
import com.textile.accounts.reports.dto.ReportsDtos.DayBookLine;
import com.textile.accounts.reports.dto.ReportsDtos.TrialBalanceLine;
import com.textile.accounts.reports.dto.ReportsDtos.TrialBalanceReport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportsService {

    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;
    private final JournalEntryRepository journalEntryRepository;

    public ReportsService(
            AccountRepository accountRepository,
            JournalLineRepository journalLineRepository,
            JournalEntryRepository journalEntryRepository
    ) {
        this.accountRepository = accountRepository;
        this.journalLineRepository = journalLineRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    @Transactional(readOnly = true)
    public TrialBalanceReport getTrialBalance(LocalDate asOfDate) {
        LocalDate date = (asOfDate != null) ? asOfDate : LocalDate.now();

        List<Account> accounts = accountRepository.findAllByOrderByCodeAsc();
        List<TrialBalanceLine> lines = new ArrayList<>();

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (Account a : accounts) {
            if (!a.isPostable()) continue;

            BigDecimal netDr = journalLineRepository.getNetDebitBalance(a.getId(), date);
            if (netDr == null) netDr = BigDecimal.ZERO;

            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;

            if (netDr.compareTo(BigDecimal.ZERO) > 0) {
                debit = netDr;
                totalDebits = totalDebits.add(debit);
            } else if (netDr.compareTo(BigDecimal.ZERO) < 0) {
                credit = netDr.abs();
                totalCredits = totalCredits.add(credit);
            }

            if (debit.compareTo(BigDecimal.ZERO) != 0 || credit.compareTo(BigDecimal.ZERO) != 0) {
                lines.add(new TrialBalanceLine(
                        a.getId(),
                        a.getCode(),
                        a.getName(),
                        a.getAccountType(),
                        debit,
                        credit
                ));
            }
        }

        BigDecimal diff = totalDebits.subtract(totalCredits).abs();
        boolean balanced = diff.compareTo(new BigDecimal("0.01")) < 0;

        return new TrialBalanceReport(
                date,
                totalDebits,
                totalCredits,
                diff,
                balanced,
                lines
        );
    }

    @Transactional(readOnly = true)
    public List<DayBookLine> getDayBook(LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        List<JournalEntry> entries = journalEntryRepository.findByEntryDateOrderByEntryNoAsc(targetDate);
        List<DayBookLine> lines = new ArrayList<>();

        for (JournalEntry entry : entries) {
            for (JournalLine line : entry.getLines()) {
                String party = "";
                if (line.getCustomer() != null) party = line.getCustomer().getName();
                else if (line.getSupplier() != null) party = line.getSupplier().getName();

                lines.add(new DayBookLine(
                        entry.getEntryNo(),
                        entry.getEntryDate(),
                        line.getAccount().getCode(),
                        line.getAccount().getName(),
                        party,
                        line.getNarration() != null ? line.getNarration() : entry.getDescription(),
                        line.getDebit(),
                        line.getCredit()
                ));
            }
        }

        return lines;
    }
}

package com.textile.accounts.ledger.service;

import com.textile.accounts.ledger.model.Account;
import com.textile.accounts.ledger.model.JournalEntry;
import com.textile.accounts.ledger.model.JournalLine;
import com.textile.accounts.ledger.repository.AccountRepository;
import com.textile.accounts.ledger.repository.JournalEntryRepository;
import com.textile.accounts.ledger.repository.JournalLineRepository;
import com.textile.accounts.masterdata.model.Customer;
import com.textile.accounts.masterdata.model.Supplier;
import com.textile.accounts.masterdata.repository.CustomerRepository;
import com.textile.accounts.masterdata.repository.SupplierRepository;
import com.textile.accounts.sales.repository.SalesInvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class IntegrityCheckService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;

    public IntegrityCheckService(
            JournalEntryRepository journalEntryRepository,
            JournalLineRepository journalLineRepository,
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            SupplierRepository supplierRepository
    ) {
        this.journalEntryRepository = journalEntryRepository;
        this.journalLineRepository = journalLineRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public IntegrityCheckResult runIntegrityCheck() {
        List<String> violations = new ArrayList<>();
        List<JournalEntry> entries = journalEntryRepository.findAll();

        BigDecimal grandTotalDebit = BigDecimal.ZERO;
        BigDecimal grandTotalCredit = BigDecimal.ZERO;

        // 1. Verify every journal entry is balanced and has >= 2 lines
        for (JournalEntry entry : entries) {
            List<JournalLine> lines = entry.getLines();
            if (lines.size() < 2) {
                violations.add(String.format("Entry %s has fewer than 2 lines (%d)", entry.getEntryNo(), lines.size()));
            }

            BigDecimal entryDebit = BigDecimal.ZERO;
            BigDecimal entryCredit = BigDecimal.ZERO;

            for (JournalLine line : lines) {
                entryDebit = entryDebit.add(line.getDebit());
                entryCredit = entryCredit.add(line.getCredit());
            }

            if (entryDebit.compareTo(entryCredit) != 0) {
                violations.add(String.format("Entry %s is unbalanced: Dr %s != Cr %s",
                        entry.getEntryNo(), entryDebit.toPlainString(), entryCredit.toPlainString()));
            }

            grandTotalDebit = grandTotalDebit.add(entryDebit);
            grandTotalCredit = grandTotalCredit.add(entryCredit);
        }

        // 2. Global trial balance equality
        BigDecimal netDiff = grandTotalDebit.subtract(grandTotalCredit);
        if (netDiff.compareTo(BigDecimal.ZERO) != 0) {
            violations.add(String.format("Global trial balance discrepancy: Total Dr %s != Total Cr %s (Diff: %s)",
                    grandTotalDebit.toPlainString(), grandTotalCredit.toPlainString(), netDiff.toPlainString()));
        }

        // 3. Customer ledger sum vs AR control
        Account arAccount = accountRepository.findBySystemRole("AR_CONTROL").orElse(null);
        if (arAccount != null) {
            BigDecimal arBalance = journalLineRepository.getNetDebitBalance(arAccount.getId(), null);
            BigDecimal sumCustomerBalances = BigDecimal.ZERO;
            for (Customer customer : customerRepository.findAll()) {
                sumCustomerBalances = sumCustomerBalances.add(journalLineRepository.getCustomerBalance(customer.getId(), null));
            }
            if (arBalance.compareTo(sumCustomerBalances) != 0) {
                violations.add(String.format("AR Control account balance (%s) does not match sum of customer balances (%s)",
                        arBalance.toPlainString(), sumCustomerBalances.toPlainString()));
            }
        }

        // 4. Supplier ledger sum vs AP control
        Account apAccount = accountRepository.findBySystemRole("AP_CONTROL").orElse(null);
        if (apAccount != null) {
            BigDecimal apBalance = journalLineRepository.getNetCreditBalance(apAccount.getId(), null);
            BigDecimal sumSupplierBalances = BigDecimal.ZERO;
            for (Supplier supplier : supplierRepository.findAll()) {
                sumSupplierBalances = sumSupplierBalances.add(journalLineRepository.getSupplierBalance(supplier.getId(), null));
            }
            if (apBalance.compareTo(sumSupplierBalances) != 0) {
                violations.add(String.format("AP Control account balance (%s) does not match sum of supplier balances (%s)",
                        apBalance.toPlainString(), sumSupplierBalances.toPlainString()));
            }
        }

        return new IntegrityCheckResult(
                violations.isEmpty(),
                entries.size(),
                grandTotalDebit,
                grandTotalCredit,
                netDiff,
                violations
        );
    }
}

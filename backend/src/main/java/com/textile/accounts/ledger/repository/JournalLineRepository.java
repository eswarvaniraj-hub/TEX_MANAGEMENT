package com.textile.accounts.ledger.repository;

import com.textile.accounts.ledger.model.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {

    @Query("SELECT COALESCE(SUM(jl.debit), 0) - COALESCE(SUM(jl.credit), 0) FROM JournalLine jl " +
           "WHERE jl.account.id = :accountId AND (:asOfDate IS NULL OR jl.journalEntry.entryDate <= :asOfDate)")
    BigDecimal getNetDebitBalance(@Param("accountId") Long accountId, @Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT COALESCE(SUM(jl.credit), 0) - COALESCE(SUM(jl.debit), 0) FROM JournalLine jl " +
           "WHERE jl.account.id = :accountId AND (:asOfDate IS NULL OR jl.journalEntry.entryDate <= :asOfDate)")
    BigDecimal getNetCreditBalance(@Param("accountId") Long accountId, @Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT COALESCE(SUM(jl.debit), 0) - COALESCE(SUM(jl.credit), 0) FROM JournalLine jl " +
           "WHERE jl.customer.id = :customerId AND (:asOfDate IS NULL OR jl.journalEntry.entryDate <= :asOfDate)")
    BigDecimal getCustomerBalance(@Param("customerId") Long customerId, @Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT COALESCE(SUM(jl.credit), 0) - COALESCE(SUM(jl.debit), 0) FROM JournalLine jl " +
           "WHERE jl.supplier.id = :supplierId AND (:asOfDate IS NULL OR jl.journalEntry.entryDate <= :asOfDate)")
    BigDecimal getSupplierBalance(@Param("supplierId") Long supplierId, @Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT COALESCE(SUM(jl.debit), 0) FROM JournalLine jl WHERE jl.account.systemRole = 'AR_CONTROL'")
    BigDecimal getTotalReceivablesDebit();

    @Query("SELECT COALESCE(SUM(jl.credit), 0) FROM JournalLine jl WHERE jl.account.systemRole = 'AP_CONTROL'")
    BigDecimal getTotalPayablesCredit();

    @Query("SELECT jl FROM JournalLine jl WHERE jl.account.id = :accountId AND " +
           "(:from IS NULL OR jl.journalEntry.entryDate >= :from) AND " +
           "(:to IS NULL OR jl.journalEntry.entryDate <= :to) " +
           "ORDER BY jl.journalEntry.entryDate ASC, jl.journalEntry.id ASC, jl.lineNo ASC")
    List<JournalLine> findAccountLedgerLines(
            @Param("accountId") Long accountId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("SELECT jl FROM JournalLine jl WHERE jl.customer.id = :customerId AND " +
           "(:from IS NULL OR jl.journalEntry.entryDate >= :from) AND " +
           "(:to IS NULL OR jl.journalEntry.entryDate <= :to) " +
           "ORDER BY jl.journalEntry.entryDate ASC, jl.journalEntry.id ASC, jl.lineNo ASC")
    List<JournalLine> findCustomerLedgerLines(
            @Param("customerId") Long customerId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("SELECT jl FROM JournalLine jl WHERE jl.supplier.id = :supplierId AND " +
           "(:from IS NULL OR jl.journalEntry.entryDate >= :from) AND " +
           "(:to IS NULL OR jl.journalEntry.entryDate <= :to) " +
           "ORDER BY jl.journalEntry.entryDate ASC, jl.journalEntry.id ASC, jl.lineNo ASC")
    List<JournalLine> findSupplierLedgerLines(
            @Param("supplierId") Long supplierId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}

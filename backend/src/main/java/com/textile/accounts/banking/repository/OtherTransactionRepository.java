package com.textile.accounts.banking.repository;

import com.textile.accounts.banking.model.OtherTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface OtherTransactionRepository extends JpaRepository<OtherTransaction, Long> {
    Optional<OtherTransaction> findByTxnNo(String txnNo);

    @Query("SELECT ot FROM OtherTransaction ot WHERE (:from IS NULL OR ot.txnDate >= :from) AND (:to IS NULL OR ot.txnDate <= :to) AND (:kind IS NULL OR ot.kind = :kind) ORDER BY ot.txnDate DESC, ot.id DESC")
    Page<OtherTransaction> searchTransactions(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("kind") String kind, Pageable pageable);
}

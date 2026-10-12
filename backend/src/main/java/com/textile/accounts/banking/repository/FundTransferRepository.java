package com.textile.accounts.banking.repository;

import com.textile.accounts.banking.model.FundTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface FundTransferRepository extends JpaRepository<FundTransfer, Long> {
    Optional<FundTransfer> findByTransferNo(String transferNo);

    @Query("SELECT ft FROM FundTransfer ft WHERE (:from IS NULL OR ft.transferDate >= :from) AND (:to IS NULL OR ft.transferDate <= :to) ORDER BY ft.transferDate DESC, ft.id DESC")
    Page<FundTransfer> searchTransfers(@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);
}

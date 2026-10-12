package com.textile.accounts.sales.repository;

import com.textile.accounts.sales.model.SalesInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, Long> {
    Optional<SalesInvoice> findByInvoiceNoAndFinancialYearId(String invoiceNo, Long financialYearId);
    Optional<SalesInvoice> findByIdempotencyKey(UUID idempotencyKey);
    boolean existsByIdempotencyKey(UUID idempotencyKey);

    @Query("SELECT si FROM SalesInvoice si WHERE " +
           "(:customerId IS NULL OR si.customer.id = :customerId) AND " +
           "(:status IS NULL OR si.status = :status) AND " +
           "(:from IS NULL OR si.invoiceDate >= :from) AND " +
           "(:to IS NULL OR si.invoiceDate <= :to) AND " +
           "(:q IS NULL OR LOWER(COALESCE(si.invoiceNo, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(si.customerNameSnapshot) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY si.invoiceDate DESC, si.id DESC")
    Page<SalesInvoice> searchSalesInvoices(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(si.grandTotal), 0) FROM SalesInvoice si WHERE si.status = 'POSTED' AND (:from IS NULL OR si.invoiceDate >= :from) AND (:to IS NULL OR si.invoiceDate <= :to)")
    BigDecimal getTotalSalesForPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(si.grandTotal), 0) FROM SalesInvoice si WHERE si.status = 'POSTED' AND si.invoiceDate = :date")
    BigDecimal getTodaySales(@Param("date") LocalDate date);
}

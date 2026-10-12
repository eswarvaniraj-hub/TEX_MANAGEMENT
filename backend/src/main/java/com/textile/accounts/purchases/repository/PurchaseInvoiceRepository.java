package com.textile.accounts.purchases.repository;

import com.textile.accounts.purchases.model.PurchaseInvoice;
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
public interface PurchaseInvoiceRepository extends JpaRepository<PurchaseInvoice, Long> {
    Optional<PurchaseInvoice> findByVoucherNoAndFinancialYearId(String voucherNo, Long financialYearId);
    Optional<PurchaseInvoice> findByIdempotencyKey(UUID idempotencyKey);
    boolean existsByIdempotencyKey(UUID idempotencyKey);

    @Query("SELECT COUNT(pi) > 0 FROM PurchaseInvoice pi WHERE pi.supplier.id = :supplierId AND pi.supplierInvoiceNoNormalized = :normalizedNo AND pi.status IN ('DRAFT', 'POSTED') AND (:excludeId IS NULL OR pi.id != :excludeId)")
    boolean existsDuplicateSupplierInvoice(
            @Param("supplierId") Long supplierId,
            @Param("normalizedNo") String normalizedNo,
            @Param("excludeId") Long excludeId
    );

    @Query("SELECT pi FROM PurchaseInvoice pi WHERE " +
           "(:supplierId IS NULL OR pi.supplier.id = :supplierId) AND " +
           "(:status IS NULL OR pi.status = :status) AND " +
           "(:from IS NULL OR pi.purchaseDate >= :from) AND " +
           "(:to IS NULL OR pi.purchaseDate <= :to) AND " +
           "(:q IS NULL OR LOWER(COALESCE(pi.voucherNo, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(pi.supplierInvoiceNo) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(pi.supplierNameSnapshot) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY pi.purchaseDate DESC, pi.id DESC")
    Page<PurchaseInvoice> searchPurchaseInvoices(
            @Param("supplierId") Long supplierId,
            @Param("status") String status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(pi.grandTotal), 0) FROM PurchaseInvoice pi WHERE pi.status = 'POSTED' AND (:from IS NULL OR pi.purchaseDate >= :from) AND (:to IS NULL OR pi.purchaseDate <= :to)")
    BigDecimal getTotalPurchasesForPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);
}

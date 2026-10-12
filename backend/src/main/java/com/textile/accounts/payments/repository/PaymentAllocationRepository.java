package com.textile.accounts.payments.repository;

import com.textile.accounts.payments.model.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {

    @Query("SELECT COALESCE(SUM(pa.amount), 0) FROM PaymentAllocation pa WHERE pa.salesInvoice.id = :invoiceId AND pa.payment.status = 'POSTED'")
    BigDecimal getTotalAllocatedForSalesInvoice(@Param("invoiceId") Long invoiceId);

    @Query("SELECT COALESCE(SUM(pa.amount), 0) FROM PaymentAllocation pa WHERE pa.purchaseInvoice.id = :invoiceId AND pa.payment.status = 'POSTED'")
    BigDecimal getTotalAllocatedForPurchaseInvoice(@Param("invoiceId") Long invoiceId);

    @Query("SELECT COALESCE(SUM(pa.amount), 0) FROM PaymentAllocation pa WHERE pa.openingBalanceItem.id = :itemId AND pa.payment.status = 'POSTED'")
    BigDecimal getTotalAllocatedForOpeningItem(@Param("itemId") Long itemId);

    @Query("SELECT COUNT(pa) > 0 FROM PaymentAllocation pa WHERE pa.salesInvoice.id = :invoiceId AND pa.payment.status = 'POSTED'")
    boolean hasActiveAllocationsForSalesInvoice(@Param("invoiceId") Long invoiceId);

    @Query("SELECT COUNT(pa) > 0 FROM PaymentAllocation pa WHERE pa.purchaseInvoice.id = :invoiceId AND pa.payment.status = 'POSTED'")
    boolean hasActiveAllocationsForPurchaseInvoice(@Param("invoiceId") Long invoiceId);

    List<PaymentAllocation> findByPaymentId(Long paymentId);
}

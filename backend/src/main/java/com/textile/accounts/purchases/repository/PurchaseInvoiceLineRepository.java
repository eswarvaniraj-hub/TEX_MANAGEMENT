package com.textile.accounts.purchases.repository;

import com.textile.accounts.purchases.model.PurchaseInvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseInvoiceLineRepository extends JpaRepository<PurchaseInvoiceLine, Long> {
    List<PurchaseInvoiceLine> findByPurchaseInvoiceIdOrderByLineNoAsc(Long invoiceId);
}

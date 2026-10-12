package com.textile.accounts.sales.repository;

import com.textile.accounts.sales.model.SalesInvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesInvoiceLineRepository extends JpaRepository<SalesInvoiceLine, Long> {
    List<SalesInvoiceLine> findBySalesInvoiceIdOrderByLineNoAsc(Long invoiceId);
}

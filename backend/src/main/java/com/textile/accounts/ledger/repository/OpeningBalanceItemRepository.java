package com.textile.accounts.ledger.repository;

import com.textile.accounts.ledger.model.OpeningBalanceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OpeningBalanceItemRepository extends JpaRepository<OpeningBalanceItem, Long> {
    List<OpeningBalanceItem> findByCustomerIdAndStatus(Long customerId, String status);
    List<OpeningBalanceItem> findBySupplierIdAndStatus(Long supplierId, String status);
}

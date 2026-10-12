package com.textile.accounts.banking.repository;

import com.textile.accounts.banking.model.PaymentAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, Long> {
    Optional<PaymentAccount> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    Optional<PaymentAccount> findByAccountId(Long accountId);

    @Query("SELECT pa FROM PaymentAccount pa WHERE (:q IS NULL OR LOWER(pa.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(COALESCE(pa.bankName, '')) LIKE LOWER(CONCAT('%', :q, '%'))) AND (:active IS NULL OR pa.active = :active) AND (:kind IS NULL OR pa.kind = :kind)")
    Page<PaymentAccount> searchPaymentAccounts(@Param("q") String q, @Param("active") Boolean active, @Param("kind") String kind, Pageable pageable);

    List<PaymentAccount> findByActiveTrue();
}

package com.textile.accounts.ledger.repository;

import com.textile.accounts.ledger.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByCodeIgnoreCase(String code);
    Optional<Account> findBySystemRole(String systemRole);
    boolean existsByCodeIgnoreCase(String code);

    @Query("SELECT a FROM Account a WHERE (:q IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(a.code) LIKE LOWER(CONCAT('%', :q, '%'))) AND (:active IS NULL OR a.active = :active) AND (:accountType IS NULL OR a.accountType = :accountType)")
    Page<Account> searchAccounts(@Param("q") String q, @Param("active") Boolean active, @Param("accountType") String accountType, Pageable pageable);

    List<Account> findAllByOrderByCodeAsc();
}

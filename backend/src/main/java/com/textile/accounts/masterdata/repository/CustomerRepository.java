package com.textile.accounts.masterdata.repository;

import com.textile.accounts.masterdata.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    @Query("SELECT c FROM Customer c WHERE (:q IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(COALESCE(c.gstin, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(COALESCE(c.phone, '')) LIKE LOWER(CONCAT('%', :q, '%'))) AND (:active IS NULL OR c.active = :active)")
    Page<Customer> searchCustomers(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);

    @Query("SELECT MAX(CAST(SUBSTRING(c.code, 2) AS int)) FROM Customer c WHERE c.code LIKE 'C%'")
    Integer findMaxCustomerNumericSuffix();
}

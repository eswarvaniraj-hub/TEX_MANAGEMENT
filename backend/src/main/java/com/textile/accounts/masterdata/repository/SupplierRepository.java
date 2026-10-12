package com.textile.accounts.masterdata.repository;

import com.textile.accounts.masterdata.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    @Query("SELECT s FROM Supplier s WHERE (:q IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.code) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(COALESCE(s.gstin, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(COALESCE(s.phone, '')) LIKE LOWER(CONCAT('%', :q, '%'))) AND (:active IS NULL OR s.active = :active)")
    Page<Supplier> searchSuppliers(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);

    @Query("SELECT MAX(CAST(SUBSTRING(s.code, 2) AS int)) FROM Supplier s WHERE s.code LIKE 'S%'")
    Integer findMaxSupplierNumericSuffix();
}

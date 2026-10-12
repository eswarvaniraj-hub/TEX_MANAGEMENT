package com.textile.accounts.masterdata.repository;

import com.textile.accounts.masterdata.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    @Query("SELECT p FROM Product p WHERE (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.code) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(COALESCE(p.hsnCode, '')) LIKE LOWER(CONCAT('%', :q, '%'))) AND (:active IS NULL OR p.active = :active)")
    Page<Product> searchProducts(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);

    @Query("SELECT MAX(CAST(SUBSTRING(p.code, 2) AS int)) FROM Product p WHERE p.code LIKE 'P%'")
    Integer findMaxProductNumericSuffix();
}

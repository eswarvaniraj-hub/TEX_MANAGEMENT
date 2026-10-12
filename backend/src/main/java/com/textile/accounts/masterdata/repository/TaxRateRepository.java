package com.textile.accounts.masterdata.repository;

import com.textile.accounts.masterdata.model.TaxRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {
    Optional<TaxRate> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);

    @Query("SELECT t FROM TaxRate t WHERE (:q IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))) AND (:active IS NULL OR t.active = :active)")
    Page<TaxRate> searchTaxRates(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);

    List<TaxRate> findAllByActiveOrderByRatePercentAsc(boolean active);
    List<TaxRate> findAllByOrderByRatePercentAsc();
}

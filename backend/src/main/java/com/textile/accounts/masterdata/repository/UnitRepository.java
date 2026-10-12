package com.textile.accounts.masterdata.repository;

import com.textile.accounts.masterdata.model.Unit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {
    Optional<Unit> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    @Query("SELECT u FROM Unit u WHERE (:q IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.code) LIKE LOWER(CONCAT('%', :q, '%'))) AND (:active IS NULL OR u.active = :active)")
    Page<Unit> searchUnits(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);

    List<Unit> findAllByActive(boolean active);
    List<Unit> findAllByOrderByCodeAsc();
}

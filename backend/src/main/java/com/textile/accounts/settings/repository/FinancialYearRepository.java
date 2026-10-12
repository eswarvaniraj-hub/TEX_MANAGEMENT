package com.textile.accounts.settings.repository;

import com.textile.accounts.settings.model.FinancialYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface FinancialYearRepository extends JpaRepository<FinancialYear, Long> {
    Optional<FinancialYear> findByLabel(String label);

    @Query("SELECT fy FROM FinancialYear fy WHERE :date >= fy.startDate AND :date <= fy.endDate")
    Optional<FinancialYear> findByDate(@Param("date") LocalDate date);
}

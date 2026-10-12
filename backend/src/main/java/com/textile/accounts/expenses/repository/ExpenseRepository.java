package com.textile.accounts.expenses.repository;

import com.textile.accounts.expenses.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Optional<Expense> findByExpenseNo(String expenseNo);
    boolean existsByIdempotencyKey(java.util.UUID idempotencyKey);
    Optional<Expense> findByIdempotencyKey(java.util.UUID idempotencyKey);

    @Query("SELECT e FROM Expense e WHERE (:from IS NULL OR e.expenseDate >= :from) AND (:to IS NULL OR e.expenseDate <= :to) AND (:categoryId IS NULL OR e.category.id = :categoryId) AND (:status IS NULL OR e.status = :status) ORDER BY e.expenseDate DESC, e.id DESC")
    Page<Expense> searchExpenses(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("categoryId") Long categoryId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.status = 'POSTED' AND (:from IS NULL OR e.expenseDate >= :from) AND (:to IS NULL OR e.expenseDate <= :to)")
    BigDecimal getTotalExpensesForPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);
}

package com.textile.accounts.payments.repository;

import com.textile.accounts.payments.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentNo(String paymentNo);
    Optional<Payment> findByIdempotencyKey(UUID idempotencyKey);
    boolean existsByIdempotencyKey(UUID idempotencyKey);

    @Query("SELECT p FROM Payment p WHERE " +
           "(:direction IS NULL OR p.direction = :direction) AND " +
           "(:partyId IS NULL OR (p.direction = 'RECEIPT' AND p.customer.id = :partyId) OR (p.direction = 'PAYMENT' AND p.supplier.id = :partyId)) AND " +
           "(:from IS NULL OR p.paymentDate >= :from) AND " +
           "(:to IS NULL OR p.paymentDate <= :to) " +
           "ORDER BY p.paymentDate DESC, p.id DESC")
    Page<Payment> searchPayments(
            @Param("direction") String direction,
            @Param("partyId") Long partyId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.direction = 'RECEIPT' AND p.status = 'POSTED' AND (:from IS NULL OR p.paymentDate >= :from) AND (:to IS NULL OR p.paymentDate <= :to)")
    BigDecimal getTotalReceiptsForPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.direction = 'PAYMENT' AND p.status = 'POSTED' AND (:from IS NULL OR p.paymentDate >= :from) AND (:to IS NULL OR p.paymentDate <= :to)")
    BigDecimal getTotalPaymentsForPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);
}

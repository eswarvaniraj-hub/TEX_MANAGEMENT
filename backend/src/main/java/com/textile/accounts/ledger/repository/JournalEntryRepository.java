package com.textile.accounts.ledger.repository;

import com.textile.accounts.ledger.model.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Optional<JournalEntry> findByEntryNo(String entryNo);
    boolean existsByReversesEntryId(Long reversesEntryId);

    @Query("SELECT je FROM JournalEntry je WHERE (:from IS NULL OR je.entryDate >= :from) AND (:to IS NULL OR je.entryDate <= :to) AND (:sourceType IS NULL OR je.sourceType = :sourceType) ORDER BY je.entryDate DESC, je.id DESC")
    Page<JournalEntry> searchEntries(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("sourceType") String sourceType, Pageable pageable);

    @Query("SELECT je FROM JournalEntry je WHERE je.sourceType = :sourceType AND je.sourceId = :sourceId")
    Optional<JournalEntry> findBySourceTypeAndSourceId(@Param("sourceType") String sourceType, @Param("sourceId") Long sourceId);

    List<JournalEntry> findByEntryDateOrderByEntryNoAsc(LocalDate entryDate);
}

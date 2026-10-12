package com.textile.accounts.settings.repository;

import com.textile.accounts.settings.model.DocumentSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ds FROM DocumentSequence ds WHERE ds.docType = :docType AND ds.financialYear.id = :fyId")
    Optional<DocumentSequence> findByDocTypeAndFinancialYearIdForUpdate(
            @Param("docType") String docType,
            @Param("fyId") Long fyId
    );

    Optional<DocumentSequence> findByDocTypeAndFinancialYearId(String docType, Long fyId);
}

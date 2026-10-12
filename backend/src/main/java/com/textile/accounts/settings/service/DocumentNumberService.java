package com.textile.accounts.settings.service;

import com.textile.accounts.settings.model.DocumentSequence;
import com.textile.accounts.settings.model.FinancialYear;
import com.textile.accounts.settings.repository.DocumentSequenceRepository;
import com.textile.accounts.settings.repository.FinancialYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class DocumentNumberService {

    private final DocumentSequenceRepository sequenceRepository;
    private final FinancialYearRepository financialYearRepository;

    public DocumentNumberService(
            DocumentSequenceRepository sequenceRepository,
            FinancialYearRepository financialYearRepository
    ) {
        this.sequenceRepository = sequenceRepository;
        this.financialYearRepository = financialYearRepository;
    }

    /**
     * Atomically assigns the next gapless document number inside the caller's transaction
     * using SELECT ... FOR UPDATE on document_sequence.
     * Format: PREFIX/YY-YY/NNNN (e.g. INV/26-27/0001)
     */
    @Transactional
    public String getNextDocumentNumber(String docType, LocalDate date) {
        FinancialYear fy = financialYearRepository.findByDate(date)
                .orElseThrow(() -> new IllegalArgumentException("No financial year configured for date: " + date));

        DocumentSequence sequence = sequenceRepository.findByDocTypeAndFinancialYearIdForUpdate(docType, fy.getId())
                .orElseGet(() -> {
                    // Create sequence if not present for new FY
                    DocumentSequence newSeq = new DocumentSequence(docType, fy, docType, 1L);
                    return sequenceRepository.save(newSeq);
                });

        long currentNum = sequence.getNextNumber();
        sequence.setNextNumber(currentNum + 1);
        sequenceRepository.save(sequence);

        // Format financial year label e.g. "2026-27" -> "26-27"
        String fyShort = formatFyShort(fy.getLabel());
        return String.format("%s/%s/%04d", sequence.getPrefix(), fyShort, currentNum);
    }

    public static String formatFyShort(String fyLabel) {
        if (fyLabel != null && fyLabel.length() >= 7 && fyLabel.contains("-")) {
            String[] parts = fyLabel.split("-");
            String startYear = parts[0].length() >= 2 ? parts[0].substring(parts[0].length() - 2) : parts[0];
            return startYear + "-" + parts[1];
        }
        return fyLabel;
    }
}

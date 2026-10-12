package com.textile.accounts.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class LedgerDtos {

    public record JournalLineDto(
            int lineNo,
            Long accountId,
            String accountCode,
            String accountName,
            BigDecimal debit,
            BigDecimal credit,
            Long customerId,
            String customerName,
            Long supplierId,
            String supplierName,
            String narration
    ) {}

    public record JournalEntryResponse(
            Long id,
            String entryNo,
            LocalDate entryDate,
            String sourceType,
            Long sourceId,
            String description,
            String createdBy,
            Long reversesEntryId,
            String reversesEntryNo,
            List<JournalLineDto> lines
    ) {}

    public record PartyLedgerLine(
            LocalDate date,
            String entryNo,
            String description,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal runningBalance
    ) {}
}

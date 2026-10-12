package com.textile.accounts.banking.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class BankingDtos {

    public record FundTransferRequest(
            @NotNull(message = "Transfer date is required") LocalDate transferDate,
            @NotNull(message = "From Payment Account ID is required") Long fromPaymentAccountId,
            @NotNull(message = "To Payment Account ID is required") Long toPaymentAccountId,
            @NotNull(message = "Amount is required") BigDecimal amount,
            String reference,
            String notes
    ) {}

    public record FundTransferResponse(
            Long id,
            String transferNo,
            LocalDate transferDate,
            Long fromPaymentAccountId,
            String fromPaymentAccountName,
            Long toPaymentAccountId,
            String toPaymentAccountName,
            BigDecimal amount,
            String reference,
            String notes,
            String status,
            Long journalEntryId,
            Long reversalEntryId,
            Instant reversedAt,
            String reversalReason
    ) {}

    public record ReverseTransferRequest(
            String reason
    ) {}

    public record AccountStatementLine(
            LocalDate date,
            String entryNo,
            String description,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal runningBalance
    ) {}
}

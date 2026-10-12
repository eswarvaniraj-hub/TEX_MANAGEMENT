package com.textile.accounts.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportsDtos {

    public record TrialBalanceLine(
            Long accountId,
            String code,
            String name,
            String accountType,
            BigDecimal debit,
            BigDecimal credit
    ) {}

    public record TrialBalanceReport(
            LocalDate asOfDate,
            BigDecimal totalDebits,
            BigDecimal totalCredits,
            BigDecimal difference,
            boolean balanced,
            List<TrialBalanceLine> lines
    ) {}

    public record DayBookLine(
            String entryNo,
            LocalDate date,
            String accountCode,
            String accountName,
            String partyName,
            String narration,
            BigDecimal debit,
            BigDecimal credit
    ) {}
}

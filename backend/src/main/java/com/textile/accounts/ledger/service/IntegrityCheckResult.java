package com.textile.accounts.ledger.service;

import java.math.BigDecimal;
import java.util.List;

public record IntegrityCheckResult(
        boolean healthy,
        int totalEntriesChecked,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        BigDecimal netDifference,
        List<String> violations
) {}

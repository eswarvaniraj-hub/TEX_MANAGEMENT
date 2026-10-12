package com.textile.accounts.ledger.service;

import com.textile.accounts.ledger.model.Account;
import com.textile.accounts.masterdata.model.Customer;
import com.textile.accounts.masterdata.model.Supplier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PostingRequest(
        LocalDate entryDate,
        String sourceType,
        Long sourceId,
        String description,
        String createdBy,
        List<PostingLineRequest> lines
) {
    public record PostingLineRequest(
            Account account,
            BigDecimal debit,
            BigDecimal credit,
            Customer customer,
            Supplier supplier,
            String narration
    ) {
        public static PostingLineRequest debit(Account account, BigDecimal amount, Customer customer, Supplier supplier, String narration) {
            return new PostingLineRequest(account, amount, BigDecimal.ZERO, customer, supplier, narration);
        }

        public static PostingLineRequest credit(Account account, BigDecimal amount, Customer customer, Supplier supplier, String narration) {
            return new PostingLineRequest(account, BigDecimal.ZERO, amount, customer, supplier, narration);
        }
    }
}

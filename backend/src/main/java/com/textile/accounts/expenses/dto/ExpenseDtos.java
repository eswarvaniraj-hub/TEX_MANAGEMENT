package com.textile.accounts.expenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class ExpenseDtos {

    public record ExpenseRequest(
            @NotNull(message = "Expense date is required") LocalDate expenseDate,
            @NotNull(message = "Category ID is required") Long categoryId,
            @NotBlank(message = "Description is required") String description,
            @NotNull(message = "Amount is required") BigDecimal amount,
            @NotNull(message = "Payment account ID is required") Long paymentAccountId,
            String referenceNo,
            String notes
    ) {}

    public record ExpenseResponse(
            Long id,
            String expenseNo,
            LocalDate expenseDate,
            Long categoryId,
            String categoryName,
            String description,
            BigDecimal amount,
            Long paymentAccountId,
            String paymentAccountName,
            String referenceNo,
            String notes,
            String status,
            Long journalEntryId,
            Long reversalEntryId,
            Instant reversedAt,
            String reversalReason
    ) {}

    public record ReverseExpenseRequest(
            String reason
    ) {}
}

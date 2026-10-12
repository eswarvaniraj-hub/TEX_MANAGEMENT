package com.textile.accounts.expenses.dto;

import jakarta.validation.constraints.NotBlank;

public class ExpenseCategoryDtos {

    public record ExpenseCategoryRequest(
            @NotBlank(message = "Category name is required") String name,
            Long accountId,
            Boolean active
    ) {}

    public record ExpenseCategoryResponse(
            Long id,
            String name,
            Long accountId,
            String accountCode,
            String accountName,
            boolean active,
            Long version
    ) {}
}

package com.textile.accounts.banking.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class PaymentAccountDtos {

    public record PaymentAccountRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Kind is required (CASH or BANK)") String kind,
            Long accountId,
            String bankName,
            String accountLast4,
            Boolean active
    ) {}

    public record PaymentAccountResponse(
            Long id,
            String name,
            String kind,
            Long accountId,
            String accountCode,
            String accountName,
            String bankName,
            String accountLast4,
            boolean active,
            BigDecimal currentBalance,
            Long version
    ) {}
}

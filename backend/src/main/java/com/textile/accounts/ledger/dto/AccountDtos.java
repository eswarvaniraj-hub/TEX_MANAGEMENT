package com.textile.accounts.ledger.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class AccountDtos {

    public record AccountRequest(
            @NotBlank(message = "Account code is required") String code,
            @NotBlank(message = "Account name is required") String name,
            @NotBlank(message = "Account type is required") String accountType,
            String systemRole,
            String reportGroup,
            Long parentId,
            Boolean isPostable,
            Boolean active
    ) {}

    public record AccountResponse(
            Long id,
            String code,
            String name,
            String accountType,
            String systemRole,
            String reportGroup,
            Long parentId,
            String parentName,
            boolean isPostable,
            boolean isSystem,
            boolean active,
            BigDecimal balance,
            Long version
    ) {}
}

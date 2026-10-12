package com.textile.accounts.settings.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class SettingsDtos {

    public record BusinessProfileResponse(
            Long id,
            String legalName,
            String tradeName,
            String address,
            String stateCode,
            String gstin,
            String pan,
            String phone,
            String email,
            String invoiceFooter,
            String bankDetailsText,
            boolean gstRegistered,
            boolean roundInvoiceTotal,
            LocalDate lockDate,
            Long version
    ) {}

    public record BusinessProfileUpdateRequest(
            @NotBlank(message = "Legal name is required") String legalName,
            @NotBlank(message = "Trade name is required") String tradeName,
            @NotBlank(message = "Address is required") String address,
            @NotBlank(message = "State code is required") String stateCode,
            String gstin,
            String pan,
            @NotBlank(message = "Phone is required") String phone,
            @NotBlank(message = "Email is required") String email,
            String invoiceFooter,
            String bankDetailsText,
            Boolean gstRegistered,
            Boolean roundInvoiceTotal,
            Long version
    ) {}

    public record LockDateUpdateRequest(
            LocalDate lockDate,
            Long version
    ) {}
}

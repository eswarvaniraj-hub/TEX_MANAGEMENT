package com.textile.accounts.masterdata.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class MasterDataDtos {

    // --- Unit DTOs ---
    public record UnitRequest(
            String code,
            @NotBlank(message = "Name is required") String name,
            @Min(value = 0, message = "Decimal places must be >= 0")
            @Max(value = 3, message = "Decimal places must be <= 3")
            int decimalPlaces,
            Boolean active
    ) {}

    public record UnitResponse(
            Long id,
            String code,
            String name,
            int decimalPlaces,
            boolean active,
            Long version
    ) {}

    // --- Tax Rate DTOs ---
    public record TaxRateRequest(
            @NotBlank(message = "Name is required") String name,
            @NotNull(message = "Rate percent is required")
            @Min(value = 0, message = "Rate percent must be >= 0")
            @Max(value = 100, message = "Rate percent must be <= 100")
            BigDecimal ratePercent,
            Boolean active
    ) {}

    public record TaxRateResponse(
            Long id,
            String name,
            BigDecimal ratePercent,
            boolean active,
            Long version
    ) {}

    // --- Product DTOs ---
    public record ProductRequest(
            String code,
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Product type is required") String productType,
            @NotNull(message = "Unit ID is required") Long unitId,
            String hsnCode,
            Long defaultTaxRateId,
            BigDecimal defaultSaleRate,
            BigDecimal defaultPurchaseRate,
            String description,
            Boolean active
    ) {}

    public record ProductResponse(
            Long id,
            String code,
            String name,
            String productType,
            Long unitId,
            String unitCode,
            int unitDecimals,
            String hsnCode,
            Long defaultTaxRateId,
            BigDecimal defaultTaxRatePercent,
            BigDecimal defaultSaleRate,
            BigDecimal defaultPurchaseRate,
            String description,
            boolean active,
            Long version
    ) {}

    // --- Customer DTOs ---
    public record CustomerRequest(
            String code,
            @NotBlank(message = "Name is required") String name,
            String contactPerson,
            String phone,
            String email,
            String address,
            String city,
            @NotBlank(message = "State code is required") String stateCode,
            String gstin,
            String pan,
            String notes,
            Boolean active
    ) {}

    public record CustomerResponse(
            Long id,
            String code,
            String name,
            String contactPerson,
            String phone,
            String email,
            String address,
            String city,
            String stateCode,
            String stateName,
            String gstin,
            String pan,
            String notes,
            boolean active,
            Long version
    ) {}

    // --- Supplier DTOs ---
    public record SupplierRequest(
            String code,
            @NotBlank(message = "Name is required") String name,
            String supplierType,
            String contactPerson,
            String phone,
            String email,
            String address,
            String city,
            @NotBlank(message = "State code is required") String stateCode,
            String gstin,
            String pan,
            String notes,
            Boolean active
    ) {}

    public record SupplierResponse(
            Long id,
            String code,
            String name,
            String supplierType,
            String contactPerson,
            String phone,
            String email,
            String address,
            String city,
            String stateCode,
            String stateName,
            String gstin,
            String pan,
            String notes,
            boolean active,
            Long version
    ) {}
}

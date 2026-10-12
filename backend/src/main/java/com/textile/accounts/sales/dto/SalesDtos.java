package com.textile.accounts.sales.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class SalesDtos {

    public record SalesInvoiceLineRequest(
            @NotNull(message = "Product ID is required") Long productId,
            String description,
            String hsnCode,
            @NotNull(message = "Quantity is required") BigDecimal quantity,
            @NotNull(message = "Unit ID is required") Long unitId,
            @NotNull(message = "Rate is required") BigDecimal rate,
            BigDecimal discountAmount,
            BigDecimal taxRatePercent
    ) {}

    public record SalesInvoiceRequest(
            @NotNull(message = "Customer ID is required") Long customerId,
            @NotNull(message = "Invoice date is required") LocalDate invoiceDate,
            LocalDate dueDate,
            String placeOfSupplyStateCode,
            String notes,
            @NotEmpty(message = "At least one item line is required") List<SalesInvoiceLineRequest> lines
    ) {}

    public record CalculatedLineResponse(
            int lineNo,
            Long productId,
            String productName,
            String description,
            String hsnCode,
            BigDecimal quantity,
            Long unitId,
            String unitCode,
            BigDecimal rate,
            BigDecimal lineAmount,
            BigDecimal discountAmount,
            BigDecimal taxableValue,
            BigDecimal taxRatePercent,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal igst,
            BigDecimal lineTotal
    ) {}

    public record CalculationResponse(
            boolean isInterstate,
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal taxableTotal,
            BigDecimal cgstTotal,
            BigDecimal sgstTotal,
            BigDecimal igstTotal,
            BigDecimal roundOff,
            BigDecimal grandTotal,
            List<CalculatedLineResponse> lines
    ) {}

    public record SalesInvoiceResponse(
            Long id,
            String invoiceNo,
            Long customerId,
            String customerName,
            String customerGstin,
            String customerAddress,
            LocalDate invoiceDate,
            LocalDate dueDate,
            String placeOfSupplyStateCode,
            String placeOfSupplyStateName,
            boolean isInterstate,
            String status,
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal taxableTotal,
            BigDecimal cgstTotal,
            BigDecimal sgstTotal,
            BigDecimal igstTotal,
            BigDecimal roundOff,
            BigDecimal grandTotal,
            String notes,
            Long journalEntryId,
            Long reversalEntryId,
            Instant postedAt,
            String postedBy,
            Instant reversedAt,
            String reversalReason,
            Long version,
            List<CalculatedLineResponse> lines
    ) {}

    public record ReverseRequest(
            String reason
    ) {}
}

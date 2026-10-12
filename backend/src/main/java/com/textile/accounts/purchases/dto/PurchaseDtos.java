package com.textile.accounts.purchases.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class PurchaseDtos {

    public record PurchaseInvoiceLineRequest(
            @NotNull(message = "Product ID is required") Long productId,
            String description,
            String hsnCode,
            @NotNull(message = "Quantity is required") BigDecimal quantity,
            @NotNull(message = "Unit ID is required") Long unitId,
            @NotNull(message = "Rate is required") BigDecimal rate,
            BigDecimal discountAmount,
            BigDecimal taxRatePercent
    ) {}

    public record PurchaseInvoiceRequest(
            @NotNull(message = "Supplier ID is required") Long supplierId,
            @NotBlank(message = "Supplier Invoice No is required") String supplierInvoiceNo,
            @NotNull(message = "Supplier Invoice Date is required") LocalDate supplierInvoiceDate,
            @NotNull(message = "Purchase Date is required") LocalDate purchaseDate,
            String notes,
            @NotEmpty(message = "At least one item line is required") List<PurchaseInvoiceLineRequest> lines
    ) {}

    public record CalculatedPurchaseLineResponse(
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

    public record PurchaseCalculationResponse(
            boolean isInterstate,
            BigDecimal subtotal,
            BigDecimal discountTotal,
            BigDecimal taxableTotal,
            BigDecimal cgstTotal,
            BigDecimal sgstTotal,
            BigDecimal igstTotal,
            BigDecimal roundOff,
            BigDecimal grandTotal,
            List<CalculatedPurchaseLineResponse> lines
    ) {}

    public record PurchaseInvoiceResponse(
            Long id,
            String voucherNo,
            Long supplierId,
            String supplierName,
            String supplierGstin,
            String supplierAddress,
            String supplierInvoiceNo,
            LocalDate supplierInvoiceDate,
            LocalDate purchaseDate,
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
            List<CalculatedPurchaseLineResponse> lines
    ) {}

    public record ReversePurchaseRequest(
            String reason
    ) {}
}

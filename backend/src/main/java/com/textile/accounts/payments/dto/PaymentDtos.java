package com.textile.accounts.payments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class PaymentDtos {

    public record PaymentAllocationRequest(
            Long salesInvoiceId,
            Long purchaseInvoiceId,
            @NotNull(message = "Allocation amount is required") BigDecimal amount
    ) {}

    public record PaymentRequest(
            @NotBlank(message = "Direction is required (RECEIPT or PAYMENT)") String direction,
            Long customerId,
            Long supplierId,
            @NotNull(message = "Payment date is required") LocalDate paymentDate,
            @NotNull(message = "Payment account is required") Long paymentAccountId,
            @NotNull(message = "Amount is required") BigDecimal amount,
            @NotBlank(message = "Payment method is required") String method,
            String referenceNo,
            String notes,
            List<PaymentAllocationRequest> allocations
    ) {}

    public record PaymentAllocationResponse(
            Long id,
            Long salesInvoiceId,
            String invoiceNo,
            Long purchaseInvoiceId,
            String voucherNo,
            BigDecimal amount
    ) {}

    public record PaymentResponse(
            Long id,
            String paymentNo,
            String direction,
            Long customerId,
            String customerName,
            Long supplierId,
            String supplierName,
            LocalDate paymentDate,
            Long paymentAccountId,
            String paymentAccountName,
            BigDecimal amount,
            String method,
            String referenceNo,
            String notes,
            String status,
            Long journalEntryId,
            Long reversalEntryId,
            Instant reversedAt,
            String reversalReason,
            List<PaymentAllocationResponse> allocations
    ) {}

    public record ReversePaymentRequest(
            String reason
    ) {}
}

package com.textile.accounts.payments.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.banking.model.PaymentAccount;
import com.textile.accounts.banking.repository.PaymentAccountRepository;
import com.textile.accounts.ledger.model.Account;
import com.textile.accounts.ledger.model.JournalEntry;
import com.textile.accounts.ledger.repository.AccountRepository;
import com.textile.accounts.ledger.service.PostingRequest;
import com.textile.accounts.ledger.service.PostingRequest.PostingLineRequest;
import com.textile.accounts.ledger.service.PostingService;
import com.textile.accounts.masterdata.model.Customer;
import com.textile.accounts.masterdata.model.Supplier;
import com.textile.accounts.masterdata.repository.CustomerRepository;
import com.textile.accounts.masterdata.repository.SupplierRepository;
import com.textile.accounts.payments.dto.PaymentDtos.PaymentAllocationRequest;
import com.textile.accounts.payments.dto.PaymentDtos.PaymentAllocationResponse;
import com.textile.accounts.payments.dto.PaymentDtos.PaymentRequest;
import com.textile.accounts.payments.dto.PaymentDtos.PaymentResponse;
import com.textile.accounts.payments.dto.PaymentDtos.ReversePaymentRequest;
import com.textile.accounts.payments.model.Payment;
import com.textile.accounts.payments.model.PaymentAllocation;
import com.textile.accounts.payments.repository.PaymentAllocationRepository;
import com.textile.accounts.payments.repository.PaymentRepository;
import com.textile.accounts.purchases.model.PurchaseInvoice;
import com.textile.accounts.purchases.repository.PurchaseInvoiceRepository;
import com.textile.accounts.sales.model.SalesInvoice;
import com.textile.accounts.sales.repository.SalesInvoiceRepository;
import com.textile.accounts.settings.model.FinancialYear;
import com.textile.accounts.settings.repository.FinancialYearRepository;
import com.textile.accounts.settings.service.DocumentNumberService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final PurchaseInvoiceRepository purchaseInvoiceRepository;
    private final FinancialYearRepository financialYearRepository;
    private final AccountRepository accountRepository;
    private final PostingService postingService;
    private final DocumentNumberService documentNumberService;
    private final AuditService auditService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentAllocationRepository allocationRepository,
            PaymentAccountRepository paymentAccountRepository,
            CustomerRepository customerRepository,
            SupplierRepository supplierRepository,
            SalesInvoiceRepository salesInvoiceRepository,
            PurchaseInvoiceRepository purchaseInvoiceRepository,
            FinancialYearRepository financialYearRepository,
            AccountRepository accountRepository,
            PostingService postingService,
            DocumentNumberService documentNumberService,
            AuditService auditService
    ) {
        this.paymentRepository = paymentRepository;
        this.allocationRepository = allocationRepository;
        this.paymentAccountRepository = paymentAccountRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.purchaseInvoiceRepository = purchaseInvoiceRepository;
        this.financialYearRepository = financialYearRepository;
        this.accountRepository = accountRepository;
        this.postingService = postingService;
        this.documentNumberService = documentNumberService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> search(String direction, Long partyId, LocalDate from, LocalDate to, Pageable pageable) {
        return paymentRepository.searchPayments(direction, partyId, from, to, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return paymentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
    }

    @Transactional
    public PaymentResponse create(PaymentRequest request, String actor) {
        postingService.validateLockDate(request.paymentDate());

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        FinancialYear fy = financialYearRepository.findByDate(request.paymentDate())
                .orElseThrow(() -> new IllegalArgumentException("No Financial Year for date: " + request.paymentDate()));

        PaymentAccount paymentAccount = paymentAccountRepository.findById(request.paymentAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Payment Account not found: " + request.paymentAccountId()));

        String direction = request.direction().toUpperCase();
        Customer customer = null;
        Supplier supplier = null;
        String seqPrefix;
        List<PostingLineRequest> postingLines = new ArrayList<>();

        if ("RECEIPT".equals(direction)) {
            seqPrefix = "RCP";
            if (request.customerId() == null) {
                throw new IllegalArgumentException("Customer ID is required for RECEIPT.");
            }
            customer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

            Account arAccount = accountRepository.findBySystemRole("AR_CONTROL")
                    .orElseThrow(() -> new IllegalStateException("Missing AR_CONTROL account"));

            // Debit Cash/Bank Payment Account
            postingLines.add(PostingLineRequest.debit(
                    paymentAccount.getAccount(),
                    request.amount(),
                    null,
                    null,
                    "Receipt via " + request.method() + (request.referenceNo() != null ? " Ref: " + request.referenceNo() : "")
            ));

            // Credit Customer AR_CONTROL
            postingLines.add(PostingLineRequest.credit(
                    arAccount,
                    request.amount(),
                    customer,
                    null,
                    "Receipt from " + customer.getName()
            ));

        } else if ("PAYMENT".equals(direction)) {
            seqPrefix = "PMT";
            if (request.supplierId() == null) {
                throw new IllegalArgumentException("Supplier ID is required for PAYMENT.");
            }
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + request.supplierId()));

            Account apAccount = accountRepository.findBySystemRole("AP_CONTROL")
                    .orElseThrow(() -> new IllegalStateException("Missing AP_CONTROL account"));

            // Debit Supplier AP_CONTROL
            postingLines.add(PostingLineRequest.debit(
                    apAccount,
                    request.amount(),
                    null,
                    supplier,
                    "Payment to " + supplier.getName()
            ));

            // Credit Cash/Bank Payment Account
            postingLines.add(PostingLineRequest.credit(
                    paymentAccount.getAccount(),
                    request.amount(),
                    null,
                    null,
                    "Payment via " + request.method() + (request.referenceNo() != null ? " Ref: " + request.referenceNo() : "")
            ));
        } else {
            throw new IllegalArgumentException("Invalid payment direction: " + request.direction());
        }

        String paymentNo = documentNumberService.getNextDocumentNumber(seqPrefix, request.paymentDate());

        PostingRequest postingRequest = new PostingRequest(
                request.paymentDate(),
                "PAYMENT",
                null,
                direction + " " + paymentNo + (customer != null ? " - " + customer.getName() : " - " + supplier.getName()),
                actor != null ? actor : "system",
                postingLines
        );

        JournalEntry journalEntry = postingService.post(postingRequest);

        Payment payment = new Payment();
        payment.setPaymentNo(paymentNo);
        payment.setFinancialYear(fy);
        payment.setDirection(direction);
        payment.setCustomer(customer);
        payment.setSupplier(supplier);
        payment.setPaymentDate(request.paymentDate());
        payment.setPaymentAccount(paymentAccount);
        payment.setAmount(request.amount());
        payment.setMethod(request.method().toUpperCase());
        payment.setReferenceNo(request.referenceNo());
        payment.setNotes(request.notes());
        payment.setStatus("POSTED");
        payment.setJournalEntry(journalEntry);
        payment.setCreatedAt(Instant.now());

        Payment saved = paymentRepository.save(payment);

        // Process bill allocations if provided
        if (request.allocations() != null && !request.allocations().isEmpty()) {
            BigDecimal allocatedSum = BigDecimal.ZERO;
            for (PaymentAllocationRequest allocReq : request.allocations()) {
                if (allocReq.amount() != null && allocReq.amount().compareTo(BigDecimal.ZERO) > 0) {
                    PaymentAllocation alloc = new PaymentAllocation();
                    alloc.setPayment(saved);
                    alloc.setAmount(allocReq.amount());
                    if (allocReq.salesInvoiceId() != null) {
                        SalesInvoice si = salesInvoiceRepository.findById(allocReq.salesInvoiceId()).orElse(null);
                        alloc.setSalesInvoice(si);
                    }
                    if (allocReq.purchaseInvoiceId() != null) {
                        PurchaseInvoice pi = purchaseInvoiceRepository.findById(allocReq.purchaseInvoiceId()).orElse(null);
                        alloc.setPurchaseInvoice(pi);
                    }
                    allocationRepository.save(alloc);
                    allocatedSum = allocatedSum.add(allocReq.amount());
                }
            }
            if (allocatedSum.compareTo(request.amount()) > 0) {
                throw new IllegalArgumentException("Total allocations (" + allocatedSum + ") cannot exceed payment amount (" + request.amount() + ")");
            }
        }

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "CREATE_PAYMENT",
                "PAYMENT",
                saved.getId().toString(),
                "Recorded " + direction + ": " + paymentNo + " (" + saved.getAmount() + ")",
                Map.of("paymentNo", paymentNo, "amount", saved.getAmount(), "direction", direction)
        );

        return toResponse(saved);
    }

    @Transactional
    public PaymentResponse reverse(Long id, ReversePaymentRequest request, String actor) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));

        if (!"POSTED".equals(payment.getStatus())) {
            throw new IllegalStateException("Only POSTED payments can be reversed.");
        }

        LocalDate reversalDate = LocalDate.now();
        postingService.validateLockDate(reversalDate);

        JournalEntry reversalEntry = postingService.reverseEntry(
                payment.getJournalEntry().getId(),
                reversalDate,
                request != null ? request.reason() : "Payment reversal",
                actor
        );

        payment.setStatus("REVERSED");
        payment.setReversalEntry(reversalEntry);
        payment.setReversedAt(Instant.now());
        payment.setReversalReason(request != null ? request.reason() : "Cancelled");

        Payment saved = paymentRepository.save(payment);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "REVERSE_PAYMENT",
                "PAYMENT",
                saved.getId().toString(),
                "Reversed " + saved.getDirection() + ": " + saved.getPaymentNo(),
                Map.of("paymentNo", saved.getPaymentNo(), "reason", saved.getReversalReason())
        );

        return toResponse(saved);
    }

    public PaymentResponse toResponse(Payment p) {
        List<PaymentAllocationResponse> allocResponses = allocationRepository.findByPaymentId(p.getId()).stream()
                .map(a -> new PaymentAllocationResponse(
                        a.getId(),
                        a.getSalesInvoice() != null ? a.getSalesInvoice().getId() : null,
                        a.getSalesInvoice() != null ? a.getSalesInvoice().getInvoiceNo() : null,
                        a.getPurchaseInvoice() != null ? a.getPurchaseInvoice().getId() : null,
                        a.getPurchaseInvoice() != null ? a.getPurchaseInvoice().getVoucherNo() : null,
                        a.getAmount()
                )).toList();

        return new PaymentResponse(
                p.getId(),
                p.getPaymentNo(),
                p.getDirection(),
                p.getCustomer() != null ? p.getCustomer().getId() : null,
                p.getCustomer() != null ? p.getCustomer().getName() : null,
                p.getSupplier() != null ? p.getSupplier().getId() : null,
                p.getSupplier() != null ? p.getSupplier().getName() : null,
                p.getPaymentDate(),
                p.getPaymentAccount().getId(),
                p.getPaymentAccount().getName(),
                p.getAmount(),
                p.getMethod(),
                p.getReferenceNo(),
                p.getNotes(),
                p.getStatus(),
                p.getJournalEntry() != null ? p.getJournalEntry().getId() : null,
                p.getReversalEntry() != null ? p.getReversalEntry().getId() : null,
                p.getReversedAt(),
                p.getReversalReason(),
                allocResponses
        );
    }
}

package com.textile.accounts.sales.service;

import com.textile.accounts.audit.service.AuditService;
import com.textile.accounts.ledger.model.Account;
import com.textile.accounts.ledger.model.JournalEntry;
import com.textile.accounts.ledger.repository.AccountRepository;
import com.textile.accounts.ledger.service.PostingRequest;
import com.textile.accounts.ledger.service.PostingRequest.PostingLineRequest;
import com.textile.accounts.ledger.service.PostingService;
import com.textile.accounts.masterdata.model.Customer;
import com.textile.accounts.masterdata.model.GstState;
import com.textile.accounts.masterdata.model.Product;
import com.textile.accounts.masterdata.model.Unit;
import com.textile.accounts.masterdata.repository.CustomerRepository;
import com.textile.accounts.masterdata.repository.GstStateRepository;
import com.textile.accounts.masterdata.repository.ProductRepository;
import com.textile.accounts.masterdata.repository.UnitRepository;
import com.textile.accounts.sales.dto.SalesDtos.CalculatedLineResponse;
import com.textile.accounts.sales.dto.SalesDtos.CalculationResponse;
import com.textile.accounts.sales.dto.SalesDtos.ReverseRequest;
import com.textile.accounts.sales.dto.SalesDtos.SalesInvoiceLineRequest;
import com.textile.accounts.sales.dto.SalesDtos.SalesInvoiceRequest;
import com.textile.accounts.sales.dto.SalesDtos.SalesInvoiceResponse;
import com.textile.accounts.sales.model.SalesInvoice;
import com.textile.accounts.sales.model.SalesInvoiceLine;
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
import java.util.Objects;

@Service
public class SalesInvoiceService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UnitRepository unitRepository;
    private final GstStateRepository gstStateRepository;
    private final FinancialYearRepository financialYearRepository;
    private final AccountRepository accountRepository;
    private final SalesInvoiceCalculationService calculationService;
    private final PostingService postingService;
    private final DocumentNumberService documentNumberService;
    private final AuditService auditService;

    public SalesInvoiceService(
            SalesInvoiceRepository salesInvoiceRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            UnitRepository unitRepository,
            GstStateRepository gstStateRepository,
            FinancialYearRepository financialYearRepository,
            AccountRepository accountRepository,
            SalesInvoiceCalculationService calculationService,
            PostingService postingService,
            DocumentNumberService documentNumberService,
            AuditService auditService
    ) {
        this.salesInvoiceRepository = salesInvoiceRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.unitRepository = unitRepository;
        this.gstStateRepository = gstStateRepository;
        this.financialYearRepository = financialYearRepository;
        this.accountRepository = accountRepository;
        this.calculationService = calculationService;
        this.postingService = postingService;
        this.documentNumberService = documentNumberService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<SalesInvoiceResponse> search(Long customerId, String status, LocalDate from, LocalDate to, String q, Pageable pageable) {
        return salesInvoiceRepository.searchSalesInvoices(customerId, status, from, to, q, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SalesInvoiceResponse getById(Long id) {
        return salesInvoiceRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Sales invoice not found: " + id));
    }

    @Transactional
    public SalesInvoiceResponse createDraft(SalesInvoiceRequest request, String actor) {
        postingService.validateLockDate(request.invoiceDate());

        FinancialYear fy = financialYearRepository.findByDate(request.invoiceDate())
                .orElseThrow(() -> new IllegalArgumentException("No Financial Year configured for: " + request.invoiceDate()));

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

        CalculationResponse calc = calculationService.calculate(request);

        String posCode = request.placeOfSupplyStateCode() != null && !request.placeOfSupplyStateCode().isBlank()
                ? request.placeOfSupplyStateCode()
                : customer.getState().getCode();
        GstState posState = gstStateRepository.findById(posCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid state code: " + posCode));

        SalesInvoice invoice = new SalesInvoice();
        invoice.setFinancialYear(fy);
        invoice.setCustomer(customer);
        invoice.setInvoiceDate(request.invoiceDate());
        invoice.setDueDate(request.dueDate() != null ? request.dueDate() : request.invoiceDate().plusDays(30));
        invoice.setPlaceOfSupplyState(posState);
        invoice.setInterstate(calc.isInterstate());
        invoice.setCustomerNameSnapshot(customer.getName());
        invoice.setCustomerGstinSnapshot(customer.getGstin());
        invoice.setCustomerAddressSnapshot(customer.getAddress());
        invoice.setStatus("DRAFT");
        invoice.setSubtotal(calc.subtotal());
        invoice.setDiscountTotal(calc.discountTotal());
        invoice.setTaxableTotal(calc.taxableTotal());
        invoice.setCgstTotal(calc.cgstTotal());
        invoice.setSgstTotal(calc.sgstTotal());
        invoice.setIgstTotal(calc.igstTotal());
        invoice.setRoundOff(calc.roundOff());
        invoice.setGrandTotal(calc.grandTotal());
        invoice.setNotes(request.notes());
        invoice.setCreatedAt(Instant.now());
        invoice.setUpdatedAt(Instant.now());

        mapLineItems(invoice, calc.lines());

        SalesInvoice saved = salesInvoiceRepository.save(invoice);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "CREATE_DRAFT",
                "SALES_INVOICE",
                saved.getId().toString(),
                "Created draft sales invoice for: " + customer.getName(),
                Map.of("customerId", customer.getId(), "grandTotal", saved.getGrandTotal())
        );

        return toResponse(saved);
    }

    @Transactional
    public SalesInvoiceResponse updateDraft(Long id, SalesInvoiceRequest request, Long submittedVersion, String actor) {
        SalesInvoice invoice = salesInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sales invoice not found: " + id));

        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Only DRAFT invoices can be edited.");
        }

        if (submittedVersion != null && !Objects.equals(invoice.getVersion(), submittedVersion)) {
            throw new IllegalStateException("OPTIMISTIC_LOCK_CONFLICT: Invoice was modified by another transaction.");
        }

        postingService.validateLockDate(request.invoiceDate());

        FinancialYear fy = financialYearRepository.findByDate(request.invoiceDate())
                .orElseThrow(() -> new IllegalArgumentException("No Financial Year configured for: " + request.invoiceDate()));

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

        CalculationResponse calc = calculationService.calculate(request);

        String posCode = request.placeOfSupplyStateCode() != null && !request.placeOfSupplyStateCode().isBlank()
                ? request.placeOfSupplyStateCode()
                : customer.getState().getCode();
        GstState posState = gstStateRepository.findById(posCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid state code: " + posCode));

        invoice.setFinancialYear(fy);
        invoice.setCustomer(customer);
        invoice.setInvoiceDate(request.invoiceDate());
        invoice.setDueDate(request.dueDate() != null ? request.dueDate() : request.invoiceDate().plusDays(30));
        invoice.setPlaceOfSupplyState(posState);
        invoice.setInterstate(calc.isInterstate());
        invoice.setCustomerNameSnapshot(customer.getName());
        invoice.setCustomerGstinSnapshot(customer.getGstin());
        invoice.setCustomerAddressSnapshot(customer.getAddress());
        invoice.setSubtotal(calc.subtotal());
        invoice.setDiscountTotal(calc.discountTotal());
        invoice.setTaxableTotal(calc.taxableTotal());
        invoice.setCgstTotal(calc.cgstTotal());
        invoice.setSgstTotal(calc.sgstTotal());
        invoice.setIgstTotal(calc.igstTotal());
        invoice.setRoundOff(calc.roundOff());
        invoice.setGrandTotal(calc.grandTotal());
        invoice.setNotes(request.notes());
        invoice.setUpdatedAt(Instant.now());

        invoice.getLines().clear();
        mapLineItems(invoice, calc.lines());

        SalesInvoice updated = salesInvoiceRepository.save(invoice);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "UPDATE_DRAFT",
                "SALES_INVOICE",
                updated.getId().toString(),
                "Updated draft sales invoice for: " + customer.getName(),
                Map.of("customerId", customer.getId(), "grandTotal", updated.getGrandTotal())
        );

        return toResponse(updated);
    }

    @Transactional
    public SalesInvoiceResponse postInvoice(Long id, String actor) {
        SalesInvoice invoice = salesInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sales invoice not found: " + id));

        if (!"DRAFT".equals(invoice.getStatus())) {
            throw new IllegalStateException("Invoice is already " + invoice.getStatus());
        }

        postingService.validateLockDate(invoice.getInvoiceDate());

        // Allocate gapless sequential invoice number
        String invoiceNo = documentNumberService.getNextDocumentNumber("INV", invoice.getInvoiceDate());
        invoice.setInvoiceNo(invoiceNo);

        // Fetch Accounts for double entry (Recipe S1)
        Account arAccount = accountRepository.findBySystemRole("AR_CONTROL")
                .orElseThrow(() -> new IllegalStateException("Missing AR_CONTROL account"));
        Account salesAccount = accountRepository.findBySystemRole("SALES")
                .orElseThrow(() -> new IllegalStateException("Missing SALES account"));

        List<PostingLineRequest> postingLines = new ArrayList<>();

        // 1. Debit Customer Receivables (AR_CONTROL)
        postingLines.add(PostingLineRequest.debit(
                arAccount,
                invoice.getGrandTotal(),
                invoice.getCustomer(),
                null,
                "Sales Invoice " + invoiceNo
        ));

        // 2. Credit Sales Revenue
        postingLines.add(PostingLineRequest.credit(
                salesAccount,
                invoice.getTaxableTotal(),
                null,
                null,
                "Sales Revenue - Invoice " + invoiceNo
        ));

        // 3. Credit Output GST
        if (invoice.getCgstTotal().compareTo(BigDecimal.ZERO) > 0) {
            Account cgstAccount = accountRepository.findBySystemRole("GST_OUTPUT_CGST")
                    .orElseThrow(() -> new IllegalStateException("Missing GST_OUTPUT_CGST account"));
            postingLines.add(PostingLineRequest.credit(cgstAccount, invoice.getCgstTotal(), null, null, "CGST Output"));
        }
        if (invoice.getSgstTotal().compareTo(BigDecimal.ZERO) > 0) {
            Account sgstAccount = accountRepository.findBySystemRole("GST_OUTPUT_SGST")
                    .orElseThrow(() -> new IllegalStateException("Missing GST_OUTPUT_SGST account"));
            postingLines.add(PostingLineRequest.credit(sgstAccount, invoice.getSgstTotal(), null, null, "SGST Output"));
        }
        if (invoice.getIgstTotal().compareTo(BigDecimal.ZERO) > 0) {
            Account igstAccount = accountRepository.findBySystemRole("GST_OUTPUT_IGST")
                    .orElseThrow(() -> new IllegalStateException("Missing GST_OUTPUT_IGST account"));
            postingLines.add(PostingLineRequest.credit(igstAccount, invoice.getIgstTotal(), null, null, "IGST Output"));
        }

        // 4. Round off adjustment
        if (invoice.getRoundOff().compareTo(BigDecimal.ZERO) != 0) {
            Account roundOffAccount = accountRepository.findBySystemRole("ROUND_OFF")
                    .orElseThrow(() -> new IllegalStateException("Missing ROUND_OFF account"));
            if (invoice.getRoundOff().compareTo(BigDecimal.ZERO) > 0) {
                // Round off added to invoice -> credit round off income
                postingLines.add(PostingLineRequest.credit(roundOffAccount, invoice.getRoundOff(), null, null, "Round off"));
            } else {
                // Round off deducted -> debit round off expense
                postingLines.add(PostingLineRequest.debit(roundOffAccount, invoice.getRoundOff().abs(), null, null, "Round off"));
            }
        }

        PostingRequest postingRequest = new PostingRequest(
                invoice.getInvoiceDate(),
                "SALES_INVOICE",
                invoice.getId(),
                "Sales Invoice: " + invoiceNo + " to " + invoice.getCustomerNameSnapshot(),
                actor != null ? actor : "system",
                postingLines
        );

        JournalEntry journalEntry = postingService.post(postingRequest);

        invoice.setJournalEntry(journalEntry);
        invoice.setStatus("POSTED");
        invoice.setPostedAt(Instant.now());
        invoice.setPostedBy(actor != null ? actor : "system");
        invoice.setUpdatedAt(Instant.now());

        SalesInvoice saved = salesInvoiceRepository.save(invoice);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "POST_INVOICE",
                "SALES_INVOICE",
                saved.getId().toString(),
                "Posted Sales Invoice: " + invoiceNo + " (JV: " + journalEntry.getEntryNo() + ")",
                Map.of("invoiceNo", invoiceNo, "grandTotal", saved.getGrandTotal())
        );

        return toResponse(saved);
    }

    @Transactional
    public SalesInvoiceResponse reverseInvoice(Long id, ReverseRequest request, String actor) {
        SalesInvoice invoice = salesInvoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sales invoice not found: " + id));

        if (!"POSTED".equals(invoice.getStatus())) {
            throw new IllegalStateException("Only POSTED invoices can be reversed.");
        }

        if (invoice.getJournalEntry() == null) {
            throw new IllegalStateException("Invoice has no associated journal entry.");
        }

        LocalDate reversalDate = LocalDate.now();
        postingService.validateLockDate(reversalDate);

        JournalEntry reversalEntry = postingService.reverseEntry(
                invoice.getJournalEntry().getId(),
                reversalDate,
                request != null ? request.reason() : "Invoice cancellation",
                actor
        );

        invoice.setStatus("REVERSED");
        invoice.setReversalEntry(reversalEntry);
        invoice.setReversedAt(Instant.now());
        invoice.setReversalReason(request != null ? request.reason() : "Cancelled");
        invoice.setUpdatedAt(Instant.now());

        SalesInvoice saved = salesInvoiceRepository.save(invoice);

        auditService.logAuditEvent(
                actor != null ? actor : "system",
                "REVERSE_INVOICE",
                "SALES_INVOICE",
                saved.getId().toString(),
                "Reversed Sales Invoice: " + saved.getInvoiceNo() + " (Reversal JV: " + reversalEntry.getEntryNo() + ")",
                Map.of("invoiceNo", saved.getInvoiceNo(), "reason", saved.getReversalReason())
        );

        return toResponse(saved);
    }

    private void mapLineItems(SalesInvoice invoice, List<CalculatedLineResponse> calcLines) {
        for (CalculatedLineResponse cl : calcLines) {
            Product product = productRepository.getReferenceById(cl.productId());
            Unit unit = unitRepository.getReferenceById(cl.unitId());

            SalesInvoiceLine line = new SalesInvoiceLine();
            line.setLineNo(cl.lineNo());
            line.setProduct(product);
            line.setDescription(cl.description());
            line.setHsnCode(cl.hsnCode());
            line.setQuantity(cl.quantity());
            line.setUnit(unit);
            line.setRate(cl.rate());
            line.setLineAmount(cl.lineAmount());
            line.setDiscountAmount(cl.discountAmount());
            line.setTaxableValue(cl.taxableValue());
            line.setTaxRatePercent(cl.taxRatePercent());
            line.setCgst(cl.cgst());
            line.setSgst(cl.sgst());
            line.setIgst(cl.igst());
            line.setLineTotal(cl.lineTotal());

            invoice.addLine(line);
        }
    }

    public SalesInvoiceResponse toResponse(SalesInvoice si) {
        List<CalculatedLineResponse> lines = si.getLines().stream().map(l -> new CalculatedLineResponse(
                l.getLineNo(),
                l.getProduct().getId(),
                l.getProduct().getName(),
                l.getDescription(),
                l.getHsnCode(),
                l.getQuantity(),
                l.getUnit().getId(),
                l.getUnit().getCode(),
                l.getRate(),
                l.getLineAmount(),
                l.getDiscountAmount(),
                l.getTaxableValue(),
                l.getTaxRatePercent(),
                l.getCgst(),
                l.getSgst(),
                l.getIgst(),
                l.getLineTotal()
        )).toList();

        return new SalesInvoiceResponse(
                si.getId(),
                si.getInvoiceNo(),
                si.getCustomer().getId(),
                si.getCustomerNameSnapshot(),
                si.getCustomerGstinSnapshot(),
                si.getCustomerAddressSnapshot(),
                si.getInvoiceDate(),
                si.getDueDate(),
                si.getPlaceOfSupplyState().getCode(),
                si.getPlaceOfSupplyState().getName(),
                si.isInterstate(),
                si.getStatus(),
                si.getSubtotal(),
                si.getDiscountTotal(),
                si.getTaxableTotal(),
                si.getCgstTotal(),
                si.getSgstTotal(),
                si.getIgstTotal(),
                si.getRoundOff(),
                si.getGrandTotal(),
                si.getNotes(),
                si.getJournalEntry() != null ? si.getJournalEntry().getId() : null,
                si.getReversalEntry() != null ? si.getReversalEntry().getId() : null,
                si.getPostedAt(),
                si.getPostedBy(),
                si.getReversedAt(),
                si.getReversalReason(),
                si.getVersion(),
                lines
        );
    }
}
